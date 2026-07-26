package com.burny.financas.investments.dto;

import com.burny.financas.investments.entity.OperationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateInvestmentOperationRequest(
        @NotNull(message = "Type is required")
        OperationType type,

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than zero")
        BigDecimal quantity,

        @NotNull(message = "Unit price is required")
        @Positive(message = "Unit price must be greater than zero")
        BigDecimal unitPrice,

        @NotNull(message = "Operation date is required")
        LocalDate operationDate
) {
}
