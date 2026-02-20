package com.proconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String displayName;
    private String headline;
    private String bio;
    private String avatarUrl;
    private String coverImageUrl;
    private LocationDTO location;
    private Boolean isVerified;
    private Boolean isAvailable;
    private BigDecimal rating;
    private Integer reviewCount;
    private BigDecimal hourlyRateMin;
    private BigDecimal hourlyRateMax;
    private String currency;
    private String email;
    private String phone;
    private String whatsapp;
    private String category;
    private List<SkillDTO> skills;
    private List<ServiceDTO> services;
    private List<SocialLinkDTO> socialLinks;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationDTO {
        private String city;
        private String state;
        private String country;
        private Boolean remote;
    }
}
