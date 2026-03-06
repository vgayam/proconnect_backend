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

    private final JobPostRepository         jobPostRepository;
    private final ProfessionalRepository    professionalRepository;
    private final BookingEventService       bookingEventService;
    private final EmailOtpService           emailOtpService;
    private final JobPostPersistenceService jobPostPersistenceService;

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Public entry point — NOT @Transactional itself so the broadcast runs
     * only AFTER the inner @Transactional saveJob() has fully committed.
     * This prevents the race where a professional receives the SSE event and
     * tries to accept the job before the DB row is visible.
     */
    public JobPostDTO createJobPost(JobPostDTO.CreateRequest req) {
        // 1. Persist inside a transaction that commits before we return
        JobPost job = jobPostPersistenceService.save(req);

        log.info("Job post committed id={} category={} lat={} lng={}",
                job.getId(), job.getCategory(), job.getLat(), job.getLng());

        // 2. Broadcast AFTER commit — job row is now visible to every reader
        int notified = broadcastToNearbyProfessionals(job);

        JobPostDTO dto = JobPostDTO.from(job);
        dto.setBroadcastCount(notified);
        return dto;
    }

    // ── Poll (multi-server safe) ───────────────────────────────────────────────

    /**
     * Returns all OPEN, non-expired job posts near a professional's location
     * matching their category. Called by GET /api/jobs/open on a poll interval.
     * Works across multiple servers — reads straight from the DB.
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
