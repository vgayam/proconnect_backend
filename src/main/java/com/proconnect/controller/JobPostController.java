package com.proconnect.controller;

import com.proconnect.dto.JobPostDTO;
import com.proconnect.service.JobPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Broadcast job post endpoints.
 * All separate from the existing /api/inquiries (direct booking) flow.
 *
 * Public:
 *   POST /api/jobs              — customer posts a job
 *
 * Authenticated (professional):
 *   POST /api/jobs/{id}/accept  — professional accepts a job
 */
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Slf4j
public class JobPostController {

    private final JobPostService jobPostService;

    /**
     * Customer posts a broadcast job request.
     * No auth required — same pattern as professional registration.
     */
    @PostMapping
    public ResponseEntity<?> postJob(@RequestBody JobPostDTO.CreateRequest req) {
        if (req.getCustomerName() == null || req.getCustomerName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Name is required"));
        }
        if (req.getCategory() == null || req.getCategory().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Category is required"));
        }
        if (req.getDescription() == null || req.getDescription().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Description is required"));
        }
        if (req.getLat() == null || req.getLng() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Location is required to find nearby professionals"));
        }
        log.info("POST /api/jobs — customer={} category={}", req.getCustomerName(), req.getCategory());
        JobPostDTO result = jobPostService.createJobPost(req);

        if (result.getBroadcastCount() == 0) {
            return ResponseEntity.status(404).body(Map.of(
                "message", "No " + req.getCategory() + " professionals found near your location. Try again later or expand your search."
            ));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Professional accepts a broadcast job.
     * professionalId is taken directly from the JWT principal — no body needed.
     * Uses optimistic locking to prevent duplicate acceptance.
     */
    @PostMapping("/{id}/accept")
    public ResponseEntity<?> acceptJob(
            @PathVariable Long id,
            @AuthenticationPrincipal Long professionalId) {

        if (professionalId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        log.info("POST /api/jobs/{}/accept — professionalId={}", id, professionalId);
        try {
            JobPostDTO result = jobPostService.acceptJob(id, professionalId);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("message", e.getMessage()));
        } catch (OptimisticLockingFailureException e) {
            return ResponseEntity.status(409).body(Map.of("message", "Job was already accepted by another professional"));
        }
    }
}
