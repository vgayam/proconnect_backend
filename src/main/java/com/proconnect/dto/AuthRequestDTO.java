package com.proconnect.dto;

import lombok.Data;

@Data
public class AuthRequestDTO {
    private String email;
    private String otp;
}
