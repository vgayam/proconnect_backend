package com.proconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
    private Long id;
    private String displayName;
    private String email;
    private String slug;
    private Boolean isAvailable;
    private Boolean isVerified;
    private String avatarUrl;
    private String headline;
}
