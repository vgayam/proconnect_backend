package com.proconnect.repository;

import com.proconnect.entity.ContactView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface ContactViewRepository extends JpaRepository<ContactView, Long> {

    @Query("SELECT COUNT(cv) FROM ContactView cv WHERE cv.viewerEmail = :email AND cv.viewedAt >= :since")
    long countByEmailSince(@Param("email") String email, @Param("since") Instant since);

    @Query("SELECT COUNT(cv) FROM ContactView cv WHERE cv.viewerIp = :ip AND cv.viewedAt >= :since")
    long countByIpSince(@Param("ip") String ip, @Param("since") Instant since);
}
