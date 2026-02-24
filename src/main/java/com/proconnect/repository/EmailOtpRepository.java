package com.proconnect.repository;

import com.proconnect.entity.EmailOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {

    /** Find the most recent unverified, unexpired OTP for an email */
    @Query("""
        SELECT o FROM EmailOtp o
        WHERE o.email = :email
          AND o.otpCode = :otpCode
          AND o.verified = false
          AND o.expiresAt > CURRENT_TIMESTAMP
        ORDER BY o.createdAt DESC
        LIMIT 1
        """)
    Optional<EmailOtp> findValidOtp(@Param("email") String email, @Param("otpCode") String otpCode);

    /** Invalidate all previous OTPs for an email when sending a new one */
    @Modifying
    @Query("UPDATE EmailOtp o SET o.verified = true WHERE o.email = :email AND o.verified = false")
    void invalidateAll(@Param("email") String email);
}
