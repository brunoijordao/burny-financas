package com.burny.financas.budgets.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * {@code limitAmount} is null for a blank budget the renewal job created that the user hasn't set
 * yet. {@code spentAmount} is never persisted — it's computed per request from the same
 * transaction-aggregation query the dashboard's category breakdown uses (see design.md Decision 1
 * / BudgetService).
 */
public record BudgetResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String icon,
        String color,
        BigDecimal limitAmount,
        BigDecimal spentAmount,
        LocalDate budgetMonth,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
