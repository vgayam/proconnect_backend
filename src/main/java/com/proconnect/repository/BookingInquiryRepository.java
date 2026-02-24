package com.proconnect.repository;

import com.proconnect.entity.BookingInquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingInquiryRepository extends JpaRepository<BookingInquiry, Long> {

    Optional<BookingInquiry> findByReviewToken(String token);

    List<BookingInquiry> findByProfessionalId(Long professionalId);
}
