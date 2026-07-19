package com.burny.financas.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregated dashboard figures for the month containing the request's {@code referenceDate}.
 * {@code futureIncome}/{@code futureExpense} are the subset of {@code monthIncome}/{@code
 * monthExpense} dated strictly after {@code referenceDate}, used by the frontend to compute the
 * month-end balance projection (see design.md Decision 3 — the projection itself is not computed
 * here, since it also needs the account balances the frontend already has from {@code GET /accounts}).
 */
public record DashboardSummaryResponse(
        String month,
        BigDecimal monthIncome,
        BigDecimal monthExpense,
        BigDecimal monthNet,
        BigDecimal futureIncome,
        BigDecimal futureExpense,
        List<CategoryBreakdownItem> categoryBreakdown,
        List<MonthlyTrendItem> monthlyTrend
) {
}
