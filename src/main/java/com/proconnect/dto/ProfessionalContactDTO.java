package com.proconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalContactDTO {
    private String email;
    private String phone;
    private String whatsapp;
}
