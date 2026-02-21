package com.proconnect.controller;

import com.proconnect.dto.SubcategoryDTO;
import com.proconnect.service.SubcategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Subcategory endpoints (replaces /api/skills).
 * Both /api/subcategories and /api/skills (legacy) are served.
 */
@RestController
@RequestMapping({"/api/subcategories", "/api/skills"})
@RequiredArgsConstructor
@Slf4j
public class SubcategoryController {

    private final SubcategoryService subcategoryService;

    @GetMapping
    public ResponseEntity<List<SubcategoryDTO>> getAllSubcategories() {
        log.info("GET /api/subcategories");
        return ResponseEntity.ok(subcategoryService.getAllSubcategories());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        log.info("GET /api/subcategories/categories");
        return ResponseEntity.ok(subcategoryService.getAllCategories());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<SubcategoryDTO>> getByCategory(@PathVariable String category) {
        log.info("GET /api/subcategories/category/{}", category);
        return ResponseEntity.ok(subcategoryService.getSubcategoriesByCategory(category));
    }

    @PostMapping
    public ResponseEntity<SubcategoryDTO> createSubcategory(@RequestBody SubcategoryDTO dto) {
        log.info("POST /api/subcategories — name={}, category={}", dto.getName(), dto.getCategory());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(subcategoryService.createSubcategory(dto));
    }
}
