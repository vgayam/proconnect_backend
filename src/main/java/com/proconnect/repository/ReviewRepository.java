package com.proconnect.repository;

import com.proconnect.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProfessionalIdOrderByCreatedAtDesc(Long professionalId);

    boolean existsByInquiryId(Long inquiryId);

    long countByProfessionalId(Long professionalId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.professional.id = :professionalId")
    Double findAverageRatingByProfessionalId(@Param("professionalId") Long professionalId);
}
