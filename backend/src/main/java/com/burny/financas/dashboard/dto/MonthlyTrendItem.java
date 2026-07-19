package com.burny.financas.dashboard.dto;

import java.math.BigDecimal;

/** Income and expense totals for one month of the 6-month trend, in {@code yyyy-MM} order. */
public record MonthlyTrendItem(
        String month,
        BigDecimal income,
        BigDecimal expense
) {
}
