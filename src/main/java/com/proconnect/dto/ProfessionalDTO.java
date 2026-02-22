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
    private String slug;
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
    /** Subcategories (formerly skills) */
    private List<SubcategoryDTO> subcategories;
    /** Kept for backwards compatibility with existing frontend */
    private List<SubcategoryDTO> skills;
    private List<ServiceDTO> services;
    private List<SocialLinkDTO> socialLinks;

    /** Areas/localities this professional serves — e.g. ["Indiranagar", "Koramangala"] */
    private List<String> serviceAreas;

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
