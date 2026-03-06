package com.proconnect.service;

import com.proconnect.dto.JobPostDTO;
import com.proconnect.entity.JobPost;
import com.proconnect.entity.Professional;
import com.proconnect.exception.ResourceNotFoundException;
import com.proconnect.repository.JobPostRepository;
import com.proconnect.repository.ProfessionalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles broadcast job posts — completely separate from the existing
 * direct-booking (BookingInquiry) flow.
 *
 * Flow:
 *  1. Customer POSTs a job → saved as OPEN, broadcast to nearby pros after commit
 *  2. Professional POSTs /accept → optimistic lock ensures only one wins
 *  3. Customer and professional both receive an email
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobPostService {

    private static final int EXPIRY_MINUTES = 30;

    private final JobPostRepository      jobPostRepository;
    private final ProfessionalRepository professionalRepository;
    private final BookingEventService    bookingEventService;
    private final EmailOtpService        emailOtpService;

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Public entry point — NOT @Transactional itself so the broadcast fires
     * only AFTER persistJobPost() has fully committed.
     * This prevents the race where a pro receives the SSE / poll event and
     * tries to accept before the DB row is visible.
     */
    public JobPostDTO createJobPost(JobPostDTO.CreateRequest req) {
        JobPost job = persistJobPost(req);   // commits its own transaction
        log.info("Job post committed id={} category={} lat={} lng={}",
                job.getId(), job.getCategory(), job.getLat(), job.getLng());
        int notified = broadcastToNearbyProfessionals(job);
        JobPostDTO dto = JobPostDTO.from(job);
        dto.setBroadcastCount(notified);
        return dto;
    }

    /**
     * Persists the job in its own REQUIRES_NEW transaction so it commits
     * immediately, before the caller broadcasts.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobPost persistJobPost(JobPostDTO.CreateRequest req) {
        JobPost job = new JobPost();
        job.setCustomerName(req.getCustomerName());
        job.setCustomerEmail(req.getCustomerEmail());
        job.setCustomerPhone(req.getCustomerPhone());
        job.setCategory(req.getCategory());
        job.setDescription(req.getDescription());
        job.setAddress(req.getAddress());
        job.setLat(req.getLat());
        job.setLng(req.getLng());
        job.setRadiusKm(5);
        job.setStatus("OPEN");
        job.setExpiresAt(LocalDateTime.now().plusMinutes(EXPIRY_MINUTES));
        return jobPostRepository.save(job);
    }

    // ── Poll (multi-server safe) ──────────────────────────────────────────────

    /**
     * Returns all OPEN, non-expired job posts near a professional's location
     * matching their category. Called every 10 s from the dashboard.
     */
    public List<JobPostDTO> getOpenJobsForProfessional(Long professionalId) {
        Professional pro = professionalRepository.findById(professionalId)
                .orElseThrow(() -> ResourceNotFoundException.professionalNotFound(professionalId));

        if (pro.getLatitude() == null || pro.getLongitude() == null || pro.getCategoryName() == null) {
            return List.of();
        }
        return jobPostRepository.pollOpenJobsNearProfessional(
                        pro.getLatitude().doubleValue(),
                        pro.getLongitude().doubleValue(),
                        5.0,
                        pro.getCategoryName())
                .stream()
                .map(JobPostDTO::from)
                .toList();
    }

    // ── Accept (race-condition safe) ──────────────────────────────────────────

    /**
     * Professional accepts a job.
     * @throws OptimisticLockingFailureException if another pro claimed it concurrently
     * @throws IllegalStateException             if the job is no longer OPEN or has expired
     */
    @Transactional
    public JobPostDTO acceptJob(Long jobId, Long professionalId) {
        JobPost job = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        if (!"OPEN".equals(job.getStatus())) {
            throw new IllegalStateException("Job is no longer available (status=" + job.getStatus() + ")");
        }
        if (job.getExpiresAt().isBefore(LocalDateTime.now())) {
            job.setStatus("EXPIRED");
            jobPostRepository.save(job);
            throw new IllegalStateException("Job has expired");
        }

        Professional pro = professionalRepository.findById(professionalId)
                .orElseThrow(() -> ResourceNotFoundException.professionalNotFound(professionalId));

        job.setStatus("ACCEPTED");
        job.setAcceptedBy(pro);
        jobPostRepository.save(job);   // @Version bumps here; concurrent save → OptimisticLockingFailureException

        String proName = pro.getDisplayName() != null ? pro.getDisplayName() : pro.getFullName();
        log.info("Job post {} accepted by professional {} ({})", jobId, professionalId, proName);

        if (job.getCustomerEmail() != null && !job.getCustomerEmail().isBlank()) {
            emailOtpService.sendJobAcceptedToCustomer(
                    job.getCustomerEmail(), job.getCustomerName(), proName,
                    pro.getPhone(), pro.getEmail());
        }
        if (pro.getEmail() != null && !pro.getEmail().isBlank()) {
            emailOtpService.sendJobAssignedToProfessional(
                    pro.getEmail(), proName,
                    job.getCustomerName(), job.getCustomerPhone(), job.getAddress(), job.getDescription());
        }
        return JobPostDTO.from(job);
    }

    // ── SSE broadcast ─────────────────────────────────────────────────────────

    private int broadcastToNearbyProfessionals(JobPost job) {
        if (job.getLat() == null || job.getLng() == null) return 0;
        List<Professional> nearby = professionalRepository.findNearbyAvailableByCategory(
                job.getLat(), job.getLng(), job.getRadiusKm(), job.getCategory());
        log.info("Broadcasting job {} to {} nearby professional(s)", job.getId(), nearby.size());
        JobPostDTO dto = JobPostDTO.from(job);
        nearby.forEach(pro -> bookingEventService.pushNewJob(pro.getId(), dto));
        return nearby.size();
    }
}
