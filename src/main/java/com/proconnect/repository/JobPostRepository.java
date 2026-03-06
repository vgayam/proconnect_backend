package com.proconnect.repository;

import com.proconnect.entity.JobPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobPostRepository extends JpaRepository<JobPost, Long> {

    /**
     * Find all OPEN job posts whose category matches AND whose location
     * is within radiusKm of the given coordinates.
     * Uses the same Haversine formula already used in ProfessionalRepository.
     */
    @Query(nativeQuery = true, value = """
        SELECT j.*
        FROM   job_posts j
        WHERE  j.status   = 'OPEN'
          AND  LOWER(j.category) = LOWER(:category)
          AND  j.lat IS NOT NULL AND j.lng IS NOT NULL
          AND  (6371 * acos(LEAST(1.0,
                   cos(radians(CAST(:lat AS FLOAT8))) * cos(radians(CAST(j.lat AS FLOAT8)))
                   * cos(radians(CAST(j.lng AS FLOAT8)) - radians(CAST(:lng AS FLOAT8)))
                   + sin(radians(CAST(:lat AS FLOAT8))) * sin(radians(CAST(j.lat AS FLOAT8)))
               ))) < CAST(:radiusKm AS FLOAT8)
        ORDER BY j.created_at DESC
        """)
    List<JobPost> findOpenJobsNearProfessional(
            @Param("lat")       double lat,
            @Param("lng")       double lng,
            @Param("radiusKm")  double radiusKm,
            @Param("category")  String category);

    /**
     * Poll variant — finds OPEN jobs near a professional matching their category.
     * Used by GET /api/jobs/open (long-poll replacement for SSE new-job events).
     * Excludes expired jobs.
     */
    @Query(nativeQuery = true, value = """
        SELECT j.*
        FROM   job_posts j
        WHERE  j.status    = 'OPEN'
          AND  j.expires_at > NOW()
          AND  LOWER(j.category) = LOWER(:category)
          AND  j.lat IS NOT NULL AND j.lng IS NOT NULL
          AND  (6371 * acos(LEAST(1.0,
                   cos(radians(CAST(:lat AS FLOAT8))) * cos(radians(CAST(j.lat AS FLOAT8)))
                   * cos(radians(CAST(j.lng AS FLOAT8)) - radians(CAST(:lng AS FLOAT8)))
                   + sin(radians(CAST(:lat AS FLOAT8))) * sin(radians(CAST(j.lat AS FLOAT8)))
               ))) < CAST(:radiusKm AS FLOAT8)
        ORDER BY j.created_at DESC
        """)
    List<JobPost> pollOpenJobsNearProfessional(
            @Param("lat")       double lat,
            @Param("lng")       double lng,
            @Param("radiusKm")  double radiusKm,
            @Param("category")  String category);

    /** All job posts accepted by a specific professional — shown in their booking dashboard. */
    List<JobPost> findByAcceptedByIdOrderByCreatedAtDesc(Long professionalId);
}
