package com.burny.financas.reports.dto;

import java.math.BigDecimal;

/**
 * {@code categoryId} is {@code null} for the uncategorized-expenses group; {@code percentage} is
 * this group's share of the period's total expense.
 */
public record CategorySpendingDto(
        Long categoryId,
        String categoryName,
        String icon,
        String color,
        BigDecimal total,
        BigDecimal percentage
) {
}
