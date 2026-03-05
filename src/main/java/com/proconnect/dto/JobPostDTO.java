package com.proconnect.dto;

import com.proconnect.entity.JobPost;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobPostDTO {

    private Long   id;
    private String customerName;
    private String category;
    private String description;
    private String address;
    private Double lat;
    private Double lng;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    /** Accepted professional name — only shown after acceptance */
    private String acceptedByName;

    public static JobPostDTO from(JobPost j) {
        JobPostDTO dto = new JobPostDTO();
        dto.id           = j.getId();
        dto.customerName = j.getCustomerName();
        dto.category     = j.getCategory();
        dto.description  = j.getDescription();
        dto.address      = j.getAddress();
        dto.lat          = j.getLat();
        dto.lng          = j.getLng();
        dto.status       = j.getStatus();
        dto.expiresAt    = j.getExpiresAt();
        dto.createdAt    = j.getCreatedAt();
        if (j.getAcceptedBy() != null) {
            dto.acceptedByName = j.getAcceptedBy().getDisplayName() != null
                ? j.getAcceptedBy().getDisplayName()
                : j.getAcceptedBy().getFullName();
        }
        return dto;
    }

    // ── Inbound request shape ─────────────────────────────────────────────────

    /** Used when a customer POSTs a new job */
    @Data
    public static class CreateRequest {
        private String customerName;
        private String customerEmail;
        private String customerPhone;
        private String category;
        private String description;
        private String address;
        private Double lat;
        private Double lng;
    }
}
