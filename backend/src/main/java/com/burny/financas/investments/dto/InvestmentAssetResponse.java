package com.burny.financas.investments.dto;

import com.burny.financas.investments.entity.AssetType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * {@code currentValue}/{@code profitabilityAmount}/{@code profitabilityPercentage} are {@code null}
 * when the asset has no recorded valuation yet — an omitted figure, not a misleading zero
 * (design.md Decision 2, spec.md "No valuation yet reports no profitability").
 */
public record InvestmentAssetResponse(
        Long id,
        String name,
        String ticker,
        AssetType type,
        Long accountId,
        String accountName,
        BigDecimal quantity,
        BigDecimal averagePrice,
        BigDecimal investedAmount,
        BigDecimal currentValue,
        BigDecimal profitabilityAmount,
        BigDecimal profitabilityPercentage,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
