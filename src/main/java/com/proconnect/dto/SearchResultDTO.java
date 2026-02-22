package com.proconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Paginated search response with facets for filtering UI.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDTO {

    private List<ProfessionalDTO> results;

    // Pagination
    private int page;
    private int pageSize;
    private long total;
    private int totalPages;

    // Echo back what was searched
    private String query;
    private String location;

    /**
     * Facets — counts per filter dimension.
     * e.g. {"Plumbing": 3, "Electrical": 2}
     */
    private Map<String, Long> categoryFacets;
    private Map<String, Long> cityFacets;
    private Map<String, Long> remoteFacets;
    /** Counts per service area — e.g. {"Indiranagar": 5, "Koramangala": 3} */
    private Map<String, Long> areaFacets;
}
