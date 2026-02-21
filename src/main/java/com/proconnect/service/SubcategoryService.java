package com.proconnect.service;

import com.proconnect.dto.SubcategoryDTO;
import com.proconnect.entity.Subcategory;
import com.proconnect.repository.SubcategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubcategoryService {

    private final SubcategoryRepository subcategoryRepository;

    public List<SubcategoryDTO> getAllSubcategories() {
        return subcategoryRepository.findAllOrderByName().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public List<String> getAllCategories() {
        return subcategoryRepository.findAllCategories();
    }

    public List<SubcategoryDTO> getSubcategoriesByCategory(String category) {
        return subcategoryRepository.findByCategory(category).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public SubcategoryDTO createSubcategory(SubcategoryDTO dto) {
        Subcategory entity = new Subcategory();
        entity.setName(dto.getName());
        entity.setCategory(dto.getCategory());
        Subcategory saved = subcategoryRepository.save(entity);
        return toDTO(saved);
    }

    private SubcategoryDTO toDTO(Subcategory entity) {
        return new SubcategoryDTO(entity.getId(), entity.getName(), entity.getCategory());
    }
}
