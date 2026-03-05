package com.proconnect.repository;

import com.proconnect.entity.BookingInquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingInquiryRepository extends JpaRepository<BookingInquiry, Long> {

    Optional<BookingInquiry> findByReviewToken(String token);

    Optional<BookingInquiry> findByCancellationToken(String cancellationToken);

    /** Only return actual bookings — exclude CONTACT_REVEAL entries created by the contact-reveal flow. */
    @Query("SELECT DISTINCT b FROM BookingInquiry b WHERE b.professional.id = :professionalId AND b.source = 'BOOKING' ORDER BY b.createdAt DESC")
    List<BookingInquiry> findByProfessionalId(@Param("professionalId") Long professionalId);
}
