package com.burny.financas.transactions.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Thin, stable wrapper around Spring Data's {@link Page} so the API contract doesn't leak
 * Spring-internal page representation details.
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
