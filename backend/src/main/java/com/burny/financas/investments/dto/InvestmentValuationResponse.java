package com.burny.financas.investments.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InvestmentValuationResponse(
        Long id,
        Long assetId,
        LocalDate valueDate,
        BigDecimal totalValue,
        LocalDateTime createdAt
) {
}
