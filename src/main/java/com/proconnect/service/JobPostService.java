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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles broadcast job posts — completely separate from the existing
 * direct-booking (BookingInquiry) flow.
 *
 * Flow:
 *  1. Customer POSTs a job → saved as OPEN, SSE broadcast to all nearby pros
 *  2. Professional PATCHes /accept → optimistic lock ensures only one wins
 *  3. Customer gets an email, pro gets an email
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobPostService {

    private static final int EXPIRY_MINUTES = 30;

    private final JobPostRepository       jobPostRepository;
    private final ProfessionalRepository  professionalRepository;
    private final BookingEventService     bookingEventService;
    private final EmailOtpService         emailOtpService;

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public JobPostDTO createJobPost(JobPostDTO.CreateRequest req) {
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
        jobPostRepository.save(job);

        log.info("Job post created id={} category={} lat={} lng={}",
                job.getId(), job.getCategory(), job.getLat(), job.getLng());

        // Broadcast SSE to all matching nearby professionals
        int notified = broadcastToNearbyProfessionals(job);

        JobPostDTO dto = JobPostDTO.from(job);
        dto.setBroadcastCount(notified);
        return dto;
    }

    // ── Accept (race-condition safe) ──────────────────────────────────────────

    /**
     * Professional accepts a job.
     * Uses optimistic locking — if two pros accept simultaneously,
     * only one succeeds; the other receives a 409.
     *
     * @throws OptimisticLockingFailureException if another pro already claimed it
     * @throws IllegalStateException             if the job is no longer OPEN
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
        // @Version field increments automatically — concurrent save throws OptimisticLockingFailureException
        jobPostRepository.save(job);

        String proName = pro.getDisplayName() != null ? pro.getDisplayName() : pro.getFullName();
        log.info("Job post {} accepted by professional {} ({})", jobId, professionalId, proName);

        // Notify the customer
        if (job.getCustomerEmail() != null && !job.getCustomerEmail().isBlank()) {
            emailOtpService.sendJobAcceptedToCustomer(
                    job.getCustomerEmail(), job.getCustomerName(), proName,
                    pro.getPhone(), pro.getEmail());
        }

        // Notify the professional
        if (pro.getEmail() != null && !pro.getEmail().isBlank()) {
            emailOtpService.sendJobAssignedToProfessional(
                    pro.getEmail(), proName,
                    job.getCustomerName(), job.getCustomerPhone(), job.getAddress(), job.getDescription());
        }

        return JobPostDTO.from(job);
    }

    // ── SSE broadcast ─────────────────────────────────────────────────────────

    /**
     * Find all available professionals within the job's radius who match the
     * category, then push an SSE "new-job" event to each one that is currently
     * connected to the dashboard stream.
     */
    private int broadcastToNearbyProfessionals(JobPost job) {
        if (job.getLat() == null || job.getLng() == null) return 0;

        List<Professional> nearby = professionalRepository.findNearbyAvailableByCategory(
                job.getLat(), job.getLng(), job.getRadiusKm(), job.getCategory());

        log.info("Broadcasting job {} to {} nearby professionals", job.getId(), nearby.size());

        JobPostDTO dto = JobPostDTO.from(job);
        for (Professional pro : nearby) {
            bookingEventService.pushNewJob(pro.getId(), dto);
        }
        return nearby.size();
    }
}
