package com.burny.financas.budgets.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record SetBudgetRequest(
        @NotNull(message = "Limit amount is required")
        @Positive(message = "Limit amount must be greater than zero")
        BigDecimal limitAmount
) {
}
