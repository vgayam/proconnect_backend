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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfessionalSearchService {

    private final ProfessionalRepository professionalRepository;
    private final ProfessionalMapper professionalMapper;

    /** Splits "plumber in indiranagar" → keyword + location hint */
    private static final Pattern LOCATION_SPLIT =
        Pattern.compile("^(.+?)\\s+(?:in|near|at|around)\\s+(.+)$", Pattern.CASE_INSENSITIVE);

    public SearchResultDTO search(ProfessionalSearchCriteria criteria) {
        int page     = Math.max(0, criteria.getPage());
        int pageSize = criteria.getPageSize() > 0 ? criteria.getPageSize() : 10;
        int offset   = page * pageSize;

        String query    = blankNull(criteria.getQuery());
        String city     = blankNull(criteria.getCity());
        String state    = blankNull(criteria.getState());
        String country  = blankNull(criteria.getCountry());
        String category = criteria.hasCategoriesFilter() ? criteria.getCategories().get(0) : null;
        String area     = blankNull(criteria.getArea());

        // ── Parse "plumber in indiranagar" style free-text queries ───────────
        if (query != null && area == null) {
            Matcher m = LOCATION_SPLIT.matcher(query);
            if (m.matches()) {
                String keyword = m.group(1).trim();
                String hint    = m.group(2).trim();
                List<String> matched = professionalRepository.findMatchingAreaName(hint);
                if (!matched.isEmpty()) {
                    log.info("Natural query '{}' → keyword='{}', area='{}'", query, keyword, matched.get(0));
                    area  = matched.get(0);
                } else {
                    log.info("Natural query '{}' → keyword='{}', area hint '{}' not found", query, keyword, hint);
                }
                query = keyword; // always strip the "in X" part from keyword search
            }
        }

        // ── Subcategory names → pg array literal  e.g. '{plumbing,tiling}' ──
        String subcategoryNames = null;
        if (criteria.hasSubcategoriesFilter()) {
            subcategoryNames = "{" + criteria.effectiveSubcategories().stream()
                .map(n -> n.replace("'", "''").toLowerCase())
                .collect(Collectors.joining(",")) + "}";
        }

        List<Professional> results = professionalRepository.searchProfessionals(
            query, city, state, country, criteria.getRemote(), criteria.getAvailable(),
            category, area, subcategoryNames, pageSize, offset);

        long total = professionalRepository.countSearchProfessionals(
            query, city, state, country, criteria.getRemote(), criteria.getAvailable(),
            category, area, subcategoryNames);

        List<ProfessionalDTO> dtos = results.stream()
            .map(professionalMapper::toDTO)
            .collect(Collectors.toList());

        return SearchResultDTO.builder()
            .results(dtos)
            .page(page)
            .pageSize(pageSize)
            .total(total)
            .totalPages((int) Math.ceil((double) total / pageSize))
            .query(criteria.getQuery())
            .location(city)
            .categoryFacets(buildFacets(professionalRepository.facetsByCategory()))
            .cityFacets(buildFacets(professionalRepository.facetsByCity()))
            .areaFacets(buildFacets(professionalRepository.facetsByServiceArea()))
            .build();
    }

    private String blankNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private Map<String, Long> buildFacets(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null) map.put(row[0].toString(), ((Number) row[1]).longValue());
        }
        return map;
    }
}
