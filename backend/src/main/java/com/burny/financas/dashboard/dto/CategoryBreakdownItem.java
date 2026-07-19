package com.burny.financas.dashboard.dto;

import java.math.BigDecimal;

/**
 * One category's expense total for the resolved month. {@code categoryId}/{@code icon}/
 * {@code color} are null for the "uncategorized" group.
 */
public record CategoryBreakdownItem(
        Long categoryId,
        String categoryName,
        String icon,
        String color,
        BigDecimal total
) {
}
