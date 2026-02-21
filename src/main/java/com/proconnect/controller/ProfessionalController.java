package com.proconnect.controller;

import com.proconnect.dto.ContactMessageDTO;
import com.proconnect.dto.ProfessionalDTO;
import com.proconnect.dto.SearchResultDTO;
import com.proconnect.service.ContactService;
import com.proconnect.service.ProfessionalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/professionals")
@RequiredArgsConstructor
public class ProfessionalController {

    private final ProfessionalService professionalService;
    private final ContactService contactService;

    /**
     * Search / list professionals.
     * Returns a paginated {@link SearchResultDTO} with facets.
     * Accepts both "subcategories" (new) and "skills" (legacy alias) params.
     */
    @GetMapping
    public ResponseEntity<SearchResultDTO> getAllProfessionals(
        @RequestParam(required = false)                     String       q,
        @RequestParam(required = false)                     String       city,
        @RequestParam(required = false)                     String       location,    // alias for city
        @RequestParam(required = false)                     String       state,
        @RequestParam(required = false)                     String       country,
        @RequestParam(required = false)                     Boolean      remote,
        @RequestParam(required = false)                     Boolean      available,
        @RequestParam(required = false)                     List<String> subcategories,
        @RequestParam(required = false)                     List<String> skills,      // legacy alias
        @RequestParam(required = false)                     List<String> categories,  // multi-value
        @RequestParam(required = false)                     String       category,    // single-value alias
        @RequestParam(defaultValue = "0")                   int          page,
        @RequestParam(defaultValue = "10")                  int          pageSize
    ) {
        // Normalise: single ?category=X and ?location=X are aliases for the list/city params
        String effectiveCity = (city != null && !city.isBlank()) ? city
                             : (location != null && !location.isBlank()) ? location : null;

        List<String> effectiveCategories = categories;
        if ((effectiveCategories == null || effectiveCategories.isEmpty())
                && category != null && !category.isBlank()) {
            effectiveCategories = List.of(category);
        }

        return ResponseEntity.ok(professionalService.searchProfessionals(
            q, effectiveCity, state, country, remote, available,
            subcategories, skills, effectiveCategories, page, pageSize));
    }

    /** Look up a professional by numeric ID */
    @GetMapping("/{id}")
    public ResponseEntity<ProfessionalDTO> getProfessionalById(@PathVariable Long id) {
        return ResponseEntity.ok(professionalService.getProfessionalById(id));
    }

    /** Look up a professional by URL slug */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ProfessionalDTO> getProfessionalBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(professionalService.getProfessionalBySlug(slug));
    }

    @PostMapping
    public ResponseEntity<ProfessionalDTO> createProfessional(@Valid @RequestBody ProfessionalDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(professionalService.createProfessional(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfessionalDTO> updateProfessional(
        @PathVariable Long id,
        @Valid @RequestBody ProfessionalDTO dto
    ) {
        return ResponseEntity.ok(professionalService.updateProfessional(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfessional(@PathVariable Long id) {
        professionalService.deleteProfessional(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/contact")
    public ResponseEntity<Void> contactProfessional(
        @PathVariable Long id,
        @Valid @RequestBody ContactMessageDTO dto
    ) {
        contactService.sendContactMessage(id, dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/cities")
    public ResponseEntity<List<String>> getDistinctCities() {
        return ResponseEntity.ok(professionalService.getDistinctCities());
    }
}
