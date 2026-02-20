package com.proconnect.service;

import com.proconnect.dto.SkillDTO;
import com.proconnect.entity.Skill;
import com.proconnect.repository.SkillRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class SkillService {
    
    private final SkillRepository skillRepository;
    
    public List<SkillDTO> getAllSkills() {
        return skillRepository.findAllOrderByName().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    public List<String> getAllCategories() {
        return skillRepository.findAllCategories();
    }
    
    public List<SkillDTO> getSkillsByCategory(String category) {
        return skillRepository.findByCategory(category).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    public SkillDTO createSkill(SkillDTO dto) {
        Skill skill = new Skill();
        skill.setName(dto.getName());
        skill.setCategory(dto.getCategory());
        
        Skill saved = skillRepository.save(skill);
        return convertToDTO(saved);
    }
    
    private SkillDTO convertToDTO(Skill entity) {
        return new SkillDTO(entity.getId(), entity.getName(), entity.getCategory());
    }
}
