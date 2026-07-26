package com.burny.financas.investments.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateInvestmentValuationRequest(
        @NotNull(message = "Value date is required")
        LocalDate valueDate,

        @NotNull(message = "Total value is required")
        @Positive(message = "Total value must be greater than zero")
        BigDecimal totalValue
) {
}
