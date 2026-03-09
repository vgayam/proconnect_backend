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

    /** Normalise common city aliases to the canonical name stored in the DB */
    private static final Map<String, String> CITY_ALIASES = Map.of(
        "bangalore",  "Bengaluru",
        "bombay",     "Mumbai",
        "madras",     "Chennai",
        "calcutta",   "Kolkata",
        "poona",      "Pune"
    );

    public SearchResultDTO search(ProfessionalSearchCriteria criteria) {
        int page     = Math.max(0, criteria.getPage());
        int pageSize = criteria.getPageSize() > 0 ? criteria.getPageSize() : 10;
        int offset   = page * pageSize;

        // ── Subcategory names → pg array literal  e.g. '{plumbing,tiling}' ──
        String subcategoryNames = null;
        if (criteria.hasSubcategoriesFilter()) {
            subcategoryNames = "{" + criteria.effectiveSubcategories().stream()
                .map(n -> n.replace("'", "''").toLowerCase())
                .collect(Collectors.joining(",")) + "}";
        }

        // ── Standard text / city / area path (lat/lng applied as additive HAVING filter) ──
        String query    = blankNull(criteria.getQuery());
        String city     = blankNull(criteria.getCity());
        // Normalise legacy city aliases e.g. "Bangalore" → "Bengaluru"
        if (city != null) {
            city = CITY_ALIASES.getOrDefault(city.toLowerCase(), city);
        }
        String state    = blankNull(criteria.getState());
        String country  = blankNull(criteria.getCountry());
        String category = criteria.hasCategoriesFilter() ? criteria.getCategories().get(0) : null;
        String area     = blankNull(criteria.getArea());
        Double lat      = criteria.hasGeoFilter() ? criteria.getLat()     : null;
        Double lng      = criteria.hasGeoFilter() ? criteria.getLng()     : null;
        double radiusKm = criteria.getRadiusKm();

        // ── Parse "plumber in indiranagar" style free-text queries ───────────
        if (query != null && area == null) {
            Matcher m = LOCATION_SPLIT.matcher(query);
            if (m.matches()) {
                // "java in indiranagar" → keyword="java", area="indiranagar"
                String keyword = m.group(1).trim();
                String hint    = m.group(2).trim();
                List<String> matched = professionalRepository.findMatchingAreaName(hint);
                if (!matched.isEmpty()) {
                    log.info("Natural query '{}' → keyword='{}', area='{}'", query, keyword, matched.get(0));
                    area  = matched.get(0);
                } else {
                    log.info("Natural query '{}' → keyword='{}', area hint '{}' not found", query, keyword, hint);
                }
                query = keyword;
            } else {
                // Fallback: "java indiranagar" (no connector word) —
                // try stripping the last 1..N words and checking if they match a known area.
                String[] words = query.split("\\s+");
                for (int i = words.length - 1; i >= 1; i--) {
                    String trailingHint = String.join(" ", java.util.Arrays.copyOfRange(words, i, words.length));
                    List<String> matched = professionalRepository.findMatchingAreaName(trailingHint);
                    if (!matched.isEmpty()) {
                        String remainingKeyword = String.join(" ", java.util.Arrays.copyOfRange(words, 0, i));
                        log.info("Fallback split query '{}' → keyword='{}', area='{}'", query, remainingKeyword, matched.get(0));
                        area  = matched.get(0);
                        query = remainingKeyword.isBlank() ? null : remainingKeyword;
                        break;
                    }
                }
            }
        }

        List<Professional> results = professionalRepository.searchProfessionals(
            query, city, state, country, criteria.getRemote(), criteria.getAvailable(),
            category, area, subcategoryNames, lat, lng, radiusKm, pageSize, offset);

        long total = professionalRepository.countSearchProfessionals(
            query, city, state, country, criteria.getRemote(), criteria.getAvailable(),
            category, area, subcategoryNames, lat, lng, radiusKm);

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
