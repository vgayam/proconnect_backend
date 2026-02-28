package com.proconnect.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContactOtpRequestDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;
}
