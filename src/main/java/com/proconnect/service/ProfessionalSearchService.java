package com.proconnect.service;

import com.proconnect.dto.ProfessionalSearchCriteria;
import com.proconnect.entity.Professional;
import com.proconnect.repository.ProfessionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfessionalSearchService {
    
    private final ProfessionalRepository professionalRepository;
    
    public List<Professional> search(ProfessionalSearchCriteria criteria) {
        List<Professional> professionals;
        
        // Search by specific skills
        if (criteria.hasSkillsFilter()) {
            professionals = professionalRepository.findBySkillsNameIn(criteria.getSkills());
        }
        // Search by categories
        else if (criteria.hasCategoriesFilter()) {
            professionals = professionalRepository.findByCategoryOrSkillsCategory(criteria.getCategories());
        }
        // General search including skills, categories, and services
        else {
            professionals = professionalRepository.searchProfessionals(
                criteria.getQuery(),
                criteria.getCity(),
                criteria.getState(),
                criteria.getCountry(),
                criteria.getRemote(),
                criteria.getAvailable()
            );
        }
        
        // Apply additional filters if needed
        if ((criteria.hasSkillsFilter() || criteria.hasCategoriesFilter()) 
            && criteria.hasLocationOrAvailabilityFilter()) {
            professionals = professionals.stream()
                .filter(p -> matchesFilters(p, criteria))
                .collect(Collectors.toList());
        }
        
        return professionals;
    }
    
    private boolean matchesFilters(Professional p, ProfessionalSearchCriteria criteria) {
        return matchesQuery(p, criteria.getQuery())
            && matchesCity(p, criteria.getCity())
            && matchesState(p, criteria.getState())
            && matchesCountry(p, criteria.getCountry())
            && matchesRemote(p, criteria.getRemote())
            && matchesAvailability(p, criteria.getAvailable());
    }
    
    private boolean matchesQuery(Professional p, String query) {
        if (query == null) {
            return true;
        }
        String lowerQuery = query.toLowerCase();
        
        // Search in basic fields
        boolean matchesBasic = matchesInField(p.getFirstName(), lowerQuery) ||
                              matchesInField(p.getLastName(), lowerQuery) ||
                              matchesInField(p.getHeadline(), lowerQuery) ||
                              matchesInField(p.getBio(), lowerQuery);
        
        // Search in skills (name and category)
        boolean matchesSkills = p.getSkills() != null && p.getSkills().stream()
            .anyMatch(skill -> matchesInField(skill.getName(), lowerQuery) ||
                              matchesInField(skill.getCategory(), lowerQuery));
        
        // Search in services (title and description)
        boolean matchesServices = p.getServices() != null && p.getServices().stream()
            .anyMatch(service -> matchesInField(service.getTitle(), lowerQuery) ||
                                matchesInField(service.getDescription(), lowerQuery));
        
        return matchesBasic || matchesSkills || matchesServices;
    }
    
    private boolean matchesInField(String fieldValue, String searchTerm) {
        return fieldValue != null && fieldValue.toLowerCase().contains(searchTerm);
    }
    
    private boolean matchesCity(Professional p, String city) {
        return city == null || (p.getCity() != null && p.getCity().equalsIgnoreCase(city));
    }
    
    private boolean matchesState(Professional p, String state) {
        return state == null || (p.getState() != null && p.getState().equalsIgnoreCase(state));
    }
    
    private boolean matchesCountry(Professional p, String country) {
        return country == null || (p.getCountry() != null && p.getCountry().equalsIgnoreCase(country));
    }
    
    private boolean matchesRemote(Professional p, Boolean remote) {
        return remote == null || (p.getRemote() != null && p.getRemote().equals(remote));
    }
    
    private boolean matchesAvailability(Professional p, Boolean available) {
        return available == null || (p.getIsAvailable() != null && p.getIsAvailable().equals(available));
    }
}
