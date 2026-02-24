package com.proconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenValidationDTO {

    private boolean valid;
    private String professionalName;
    private Long professionalId;
    private String message;
}
