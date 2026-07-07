package com.burny.financas.categories.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Deliberately has no parentCategoryId field: the parent-child relationship is never editable
 * through this operation (see specs/categories/spec.md "Cannot change parent category through
 * edit").
 */
public record UpdateCategoryRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Icon is required")
        String icon,

        @NotBlank(message = "Color is required")
        String color
) {
}
