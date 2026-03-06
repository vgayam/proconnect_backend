package com.proconnect.service;

import com.proconnect.dto.JobPostDTO;
import com.proconnect.entity.JobPost;
import com.proconnect.repository.JobPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Thin persistence-only service for JobPost.
 * Kept separate so that @Transactional commits before the caller (JobPostService)
 * triggers the SSE broadcast — ensuring the row is visible to acceptJob().
 */
@Service
@RequiredArgsConstructor
public class JobPostPersistenceService {

    private static final int EXPIRY_MINUTES = 30;

    private final JobPostRepository jobPostRepository;

    @Transactional
    public JobPost save(JobPostDTO.CreateRequest req) {
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
}
