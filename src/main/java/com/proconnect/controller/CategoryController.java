package com.proconnect.controller;

import com.proconnect.entity.Category;
import com.proconnect.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    /** GET /api/categories — returns all active categories ordered by sort_order */
    @GetMapping
    @Cacheable("categories")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        List<CategoryResponse> categories = categoryRepository
            .findByIsActiveTrueOrderBySortOrder()
            .stream()
            .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getEmoji(), c.getDescription()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(categories);
    }

    /** GET /api/categories/names — returns just the name strings (used by search facets) */
    @GetMapping("/names")
    @Cacheable("categoryNames")
    public ResponseEntity<List<String>> getCategoryNames() {
        List<String> names = categoryRepository
            .findByIsActiveTrueOrderBySortOrder()
            .stream()
            .map(Category::getName)
            .collect(Collectors.toList());
        return ResponseEntity.ok(names);
    }

    // ─── Simple response record ───────────────────────────────────────────────

    public record CategoryResponse(Long id, String name, String emoji, String description) {}
}
