package com.proconnect.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "booking_inquiries")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id", nullable = false)
    private Professional professional;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "customer_phone")
    private String customerPhone;

    /** Preferred 1-hour slot requested by the client */
    @Column(name = "preferred_date")
    private String preferredDate;

    @Column(name = "preferred_time", length = 10)
    private String preferredTime;

    @Column(name = "customer_address", length = 500)
    private String customerAddress;

    @Column(name = "customer_lat")
    private Double customerLat;

    @Column(name = "customer_lng")
    private Double customerLng;

    @Column(name = "note", length = 1000)
    private String note;

    /** PENDING | ACCEPTED | REJECTED */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    /** BOOKING | CONTACT_REVEAL — distinguishes actual bookings from contact-reveal review tokens */
    @Column(name = "source", nullable = false, length = 20)
    private String source = "BOOKING";

    @Column(name = "review_token", nullable = false, unique = true, length = 64)
    private String reviewToken;

    @Column(name = "token_used", nullable = false)
    private boolean tokenUsed = false;

    @Column(name = "token_expires_at", nullable = false)
    private LocalDateTime tokenExpiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
