package com.proconnect.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A broadcast job request posted by a customer.
 * Completely separate from the existing BookingInquiry (direct bookings).
 *
 * Status lifecycle:  OPEN → ACCEPTED (first pro to claim it) | EXPIRED (no one accepted in time)
 */
@Entity
@Table(name = "job_posts")
@Data
@NoArgsConstructor
public class JobPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "customer_phone")
    private String customerPhone;

    /** Main category — e.g. "Plumbing" */
    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "address")
    private String address;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lng")
    private Double lng;

    /** Broadcast radius in km — default 5 */
    @Column(name = "radius_km", nullable = false)
    private Integer radiusKm = 5;

    /** OPEN | ACCEPTED | EXPIRED */
    @Column(name = "status", nullable = false)
    private String status = "OPEN";

    /** The professional who accepted — null until claimed */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by_id")
    private Professional acceptedBy;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** Optimistic lock — prevents two pros accepting simultaneously */
    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;
}
