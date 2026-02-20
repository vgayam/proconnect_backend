package com.proconnect.controller;

import com.proconnect.dto.ContactMessageDTO;
import com.proconnect.dto.ProfessionalDTO;
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
    
    @GetMapping
    public ResponseEntity<List<ProfessionalDTO>> getAllProfessionals(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String city,
        @RequestParam(required = false) String state,
        @RequestParam(required = false) String country,
        @RequestParam(required = false) Boolean remote,
        @RequestParam(required = false) Boolean available,
        @RequestParam(required = false) List<String> skills,
        @RequestParam(required = false) List<String> categories
    ) {
        if (q != null || city != null || state != null || country != null || remote != null || 
            available != null || (skills != null && !skills.isEmpty()) || 
            (categories != null && !categories.isEmpty())) {
            return ResponseEntity.ok(professionalService.searchProfessionals(
                q, city, state, country, remote, available, skills, categories));
        }
        return ResponseEntity.ok(professionalService.getAllProfessionals());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ProfessionalDTO> getProfessionalById(@PathVariable Long id) {
        return ResponseEntity.ok(professionalService.getProfessionalById(id));
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
