package com.proconnect.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "contact_views")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "professional_id", nullable = false)
    private Long professionalId;

    @Column(name = "viewer_email")
    private String viewerEmail;

    @Column(name = "viewer_ip")
    private String viewerIp;

    @Column(name = "viewed_at", nullable = false)
    private Instant viewedAt;

    @PrePersist
    public void prePersist() {
        if (viewedAt == null) viewedAt = Instant.now();
    }
}
