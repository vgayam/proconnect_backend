package com.proconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InquiryResponseDTO {

    private Long inquiryId;
    private String reviewToken;
    private String professionalName;
    private String message;
}
