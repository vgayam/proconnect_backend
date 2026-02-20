package com.proconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialLinkDTO {
    private Long id;
    private String platform;
    private String url;
    private String label;
}
