package com.proconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubcategoryDTO {
    private Long id;
    /** Subcategory name — e.g. "Drain Cleaning" */
    private String name;
    /** Parent category — e.g. "Plumbing" */
    private String category;
}
