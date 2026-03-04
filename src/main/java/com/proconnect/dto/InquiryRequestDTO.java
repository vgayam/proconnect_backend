package com.proconnect.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InquiryRequestDTO {

    @NotBlank(message = "Name is required")
    private String name;

    private String email;
    private String phone;
    private String address;

    /** Customer's GPS coordinates at time of booking */
    private Double customerLat;
    private Double customerLng;

    /** Preferred 1-hour slot — e.g. "2026-03-10" and "14:00" */
    private String preferredDate;
    private String preferredTime;

    private String note;
}
