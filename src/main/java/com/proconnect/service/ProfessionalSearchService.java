package com.proconnect.service;

import com.proconnect.dto.ProfessionalDTO;
import com.proconnect.dto.ProfessionalSearchCriteria;
import com.proconnect.dto.SearchResultDTO;
import com.proconnect.entity.Professional;
import com.proconnect.mapper.ProfessionalMapper;
import com.proconnect.repository.ProfessionalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfessionalSearchService {

    private final ProfessionalRepository professionalRepository;
    private final ProfessionalMapper professionalMapper;

    /**
     * Executes a paginated, faceted search using PostgreSQL FTS + pg_trgm.
     */
    public SearchResultDTO search(ProfessionalSearchCriteria criteria) {
        int page     = Math.max(0, criteria.getPage());
        int pageSize = criteria.getPageSize() > 0 ? criteria.getPageSize() : 10;
        int offset   = page * pageSize;

        String query    = blankNull(criteria.getQuery());
        String city     = blankNull(criteria.getCity());
        String state    = blankNull(criteria.getState());
        String country  = blankNull(criteria.getCountry());
        String category = criteria.hasCategoriesFilter()
                          ? criteria.getCategories().get(0) : null;

        List<Professional> results;
        long total;

        if (criteria.hasSubcategoriesFilter()) {
            // Filter by specific subcategory names
            List<String> names = criteria.effectiveSubcategories();
            // Build a PostgreSQL text-array literal: '{val1,val2}'
            String pgArray = "{" + names.stream()
                .map(n -> n.replace("'", "''").toLowerCase())
                .collect(Collectors.joining(",")) + "}";

            results = professionalRepository.searchBySubcategoryNames(
                pgArray, query, city, criteria.getAvailable(), pageSize, offset);

            // Count via in-memory fallback (subcategory count query not modeled separately)
            total = professionalRepository.countSearchProfessionals(
                query, city, state, country, criteria.getRemote(), criteria.getAvailable(), category);
        } else {
            results = professionalRepository.searchProfessionals(
                query, city, state, country,
                criteria.getRemote(), criteria.getAvailable(), category,
                pageSize, offset);

            total = professionalRepository.countSearchProfessionals(
                query, city, state, country,
                criteria.getRemote(), criteria.getAvailable(), category);
        }

        int totalPages = (int) Math.ceil((double) total / pageSize);

        List<ProfessionalDTO> dtos = results.stream()
            .map(professionalMapper::toDTO)
            .collect(Collectors.toList());

        return SearchResultDTO.builder()
            .results(dtos)
            .page(page)
            .pageSize(pageSize)
            .total(total)
            .totalPages(totalPages)
            .query(criteria.getQuery())
            .location(city)
            .categoryFacets(buildFacets(professionalRepository.facetsByCategory()))
            .cityFacets(buildFacets(professionalRepository.facetsByCity()))
            .build();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String blankNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private Map<String, Long> buildFacets(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null) {
                map.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        }
        return map;
    }
}
