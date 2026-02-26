package com.proconnect.service;

import com.proconnect.dto.SubcategoryDTO;
import com.proconnect.entity.Subcategory;
import com.proconnect.repository.CategoryRepository;
import com.proconnect.repository.SubcategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubcategoryService {

    private final SubcategoryRepository subcategoryRepository;
    private final CategoryRepository categoryRepository;

    public List<SubcategoryDTO> getAllSubcategories() {
        return subcategoryRepository.findAllOrderByName().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public List<String> getAllCategories() {
        return subcategoryRepository.findAllCategories();
    }

    public List<SubcategoryDTO> getSubcategoriesByCategory(String category) {
        return subcategoryRepository.findByCategoryName(category).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public SubcategoryDTO createSubcategory(SubcategoryDTO dto) {
        Subcategory entity = new Subcategory();
        entity.setName(dto.getName());
        categoryRepository.findByName(dto.getCategory())
            .ifPresent(entity::setCategory);
        Subcategory saved = subcategoryRepository.save(entity);
        return toDTO(saved);
    }

    private SubcategoryDTO toDTO(Subcategory entity) {
        return new SubcategoryDTO(entity.getId(), entity.getName(), entity.getCategoryName());
    }
}
