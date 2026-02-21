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
    /** Subcategory names to filter on (formerly "skills") */
    private List<String> subcategories;
    /** Kept for backwards-compat — callers may still pass "skills" */
    private List<String> skills;
    private List<String> categories;
    @Builder.Default private int page = 0;
    @Builder.Default private int pageSize = 10;

    public boolean hasAnyFilter() {
        return query != null || city != null || state != null || country != null ||
               remote != null || available != null ||
               hasSubcategoriesFilter() ||
               (categories != null && !categories.isEmpty());
    }

    public boolean hasSubcategoriesFilter() {
        return (subcategories != null && !subcategories.isEmpty())
            || (skills != null && !skills.isEmpty());
    }

    /** @deprecated use hasSubcategoriesFilter() */
    @Deprecated
    public boolean hasSkillsFilter() {
        return hasSubcategoriesFilter();
    }

    public boolean hasCategoriesFilter() {
        return categories != null && !categories.isEmpty();
    }

    public boolean hasLocationOrAvailabilityFilter() {
        return query != null || city != null || state != null ||
               country != null || remote != null || available != null;
    }

    /** Combined subcategory list (merges subcategories + legacy skills param) */
    public List<String> effectiveSubcategories() {
        if (subcategories != null && !subcategories.isEmpty()) return subcategories;
        return skills;
    }
}
