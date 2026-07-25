package com.burny.financas.planning.dto;

import com.burny.financas.planning.entity.PlanningEntryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePlanningEntryRequest(
        @NotNull(message = "Type is required")
        PlanningEntryType type,

        @NotNull(message = "Account is required")
        Long accountId,

        Long categoryId,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Due date is required")
        LocalDate dueDate
) {
}
