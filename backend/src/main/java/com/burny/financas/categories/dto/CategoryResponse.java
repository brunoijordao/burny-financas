package com.burny.financas.categories.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * {@code subcategories} is populated only when returned from a listing that nests active
 * subcategories under their top-level parent; it is an empty list for a subcategory itself or a
 * single-category lookup.
 */
public record CategoryResponse(
        Long id,
        String name,
        String icon,
        String color,
        Long parentCategoryId,
        boolean defaultCategory,
        boolean active,
        List<CategoryResponse> subcategories,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
