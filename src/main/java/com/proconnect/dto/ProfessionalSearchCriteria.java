package com.proconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalSearchCriteria {
    private String query;
    private String city;
    private String state;
    private String country;
    private Boolean remote;
    private Boolean available;
    private List<String> skills;
    private List<String> categories;
    
    public boolean hasAnyFilter() {
        return query != null || city != null || state != null || country != null || 
               remote != null || available != null || 
               (skills != null && !skills.isEmpty()) || 
               (categories != null && !categories.isEmpty());
    }
    
    public boolean hasSkillsFilter() {
        return skills != null && !skills.isEmpty();
    }
    
    public boolean hasCategoriesFilter() {
        return categories != null && !categories.isEmpty();
    }
    
    public boolean hasLocationOrAvailabilityFilter() {
        return query != null || city != null || state != null || 
               country != null || remote != null || available != null;
    }
}
