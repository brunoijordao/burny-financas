package com.burny.financas.investments.dto;

import java.math.BigDecimal;

/**
 * {@code profitabilityPercentage} is {@code null} only when {@code totalInvested} is zero (no
 * assets/operations yet), since a percentage over zero invested is undefined.
 */
public record PortfolioSummaryResponse(
        BigDecimal totalInvested,
        BigDecimal totalCurrentValue,
        BigDecimal profitabilityAmount,
        BigDecimal profitabilityPercentage
) {
}
