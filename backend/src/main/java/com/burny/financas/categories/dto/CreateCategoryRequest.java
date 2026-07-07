package com.burny.financas.categories.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Icon is required")
        String icon,

        @NotBlank(message = "Color is required")
        String color,

        Long parentCategoryId
) {
}
