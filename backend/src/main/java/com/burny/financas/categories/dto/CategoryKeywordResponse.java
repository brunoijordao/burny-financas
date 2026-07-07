package com.burny.financas.categories.dto;

import java.time.LocalDateTime;

public record CategoryKeywordResponse(
        Long id,
        Long categoryId,
        String keyword,
        LocalDateTime createdAt
) {
}
