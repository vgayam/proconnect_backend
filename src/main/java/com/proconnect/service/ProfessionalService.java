package com.proconnect.service;

import com.proconnect.dto.*;
import com.proconnect.entity.Professional;
import com.proconnect.exception.ResourceNotFoundException;
import com.proconnect.mapper.ProfessionalMapper;
import com.proconnect.repository.ProfessionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfessionalService {

    private final ProfessionalRepository professionalRepository;
    private final ProfessionalMapper professionalMapper;
    private final ProfessionalSearchService searchService;

    public List<ProfessionalDTO> getAllProfessionals() {
        return professionalRepository.findAll().stream()
            .map(professionalMapper::toDTO)
            .collect(Collectors.toList());
    }

    public ProfessionalDTO getProfessionalById(Long id) {
        Professional professional = professionalRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.professionalNotFound(id));
        return professionalMapper.toDTO(professional);
    }

    public ProfessionalDTO getProfessionalBySlug(String slug) {
        Professional professional = professionalRepository.findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Professional not found with slug: " + slug));
        return professionalMapper.toDTO(professional);
    }

    /**
     * Unified search — returns a paginated {@link SearchResultDTO} with facets.
     * Accepts both "subcategories" (new) and "skills" (legacy alias).
     */
    public SearchResultDTO searchProfessionals(ProfessionalSearchCriteria criteria) {
        return searchService.search(criteria);
    }

    @Transactional
    public ProfessionalDTO createProfessional(ProfessionalDTO dto) {
        Professional professional = professionalMapper.toEntity(dto);
        Professional saved = professionalRepository.save(professional);
        return professionalMapper.toDTO(saved);
    }

    @Transactional
    public ProfessionalDTO updateProfessional(Long id, ProfessionalDTO dto) {
        Professional professional = professionalRepository.findById(id)
            .orElseThrow(() -> ResourceNotFoundException.professionalNotFound(id));

        professionalMapper.updateEntityFromDTO(professional, dto);
        Professional updated = professionalRepository.save(professional);
        return professionalMapper.toDTO(updated);
    }

    public void deleteProfessional(Long id) {
        if (!professionalRepository.existsById(id)) {
            throw ResourceNotFoundException.professionalNotFound(id);
        }
        professionalRepository.deleteById(id);
    }

    public List<String> getDistinctCities() {
        return professionalRepository.findDistinctCities();
    }
}
