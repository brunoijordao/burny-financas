package com.burny.financas.categories.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryKeywordRequest(
        @NotBlank(message = "Keyword is required")
        String keyword
) {
}
