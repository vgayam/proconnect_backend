package com.proconnect.controller;

import com.proconnect.dto.SkillDTO;
import com.proconnect.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {
    
    private final SkillService skillService;
    
    @GetMapping
    public ResponseEntity<List<SkillDTO>> getAllSkills() {
        return ResponseEntity.ok(skillService.getAllSkills());
    }
    
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        return ResponseEntity.ok(skillService.getAllCategories());
    }
    
    @GetMapping("/category/{category}")
    public ResponseEntity<List<SkillDTO>> getSkillsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(skillService.getSkillsByCategory(category));
    }
    
    @PostMapping
    public ResponseEntity<SkillDTO> createSkill(@RequestBody SkillDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(skillService.createSkill(dto));
    }
}
