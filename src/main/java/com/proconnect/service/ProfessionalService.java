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
    
    public List<ProfessionalDTO> searchProfessionals(String query, String city, String state, 
                                                      String country, Boolean remote, Boolean available, 
                                                      List<String> skills, List<String> categories) {
        ProfessionalSearchCriteria criteria = ProfessionalSearchCriteria.builder()
            .query(query)
            .city(city)
            .state(state)
            .country(country)
            .remote(remote)
            .available(available)
            .skills(skills)
            .categories(categories)
            .build();
        
        return searchService.search(criteria).stream()
            .map(professionalMapper::toDTO)
            .collect(Collectors.toList());
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
