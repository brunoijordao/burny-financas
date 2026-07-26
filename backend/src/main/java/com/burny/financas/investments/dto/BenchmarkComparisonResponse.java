package com.burny.financas.investments.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * {@code benchmarkPercentage} is echoed back as typed in by the caller, never fetched from an
 * external source (design.md Decision 3 — out of scope for this change). {@code
 * portfolioReturnPercentage} is {@code null} when the portfolio's total value at {@code
 * periodStart} was zero (undefined percentage).
 */
public record BenchmarkComparisonResponse(
        BenchmarkType benchmarkType,
        BigDecimal benchmarkPercentage,
        BigDecimal portfolioReturnPercentage,
        LocalDate periodStart,
        LocalDate periodEnd
) {
}
