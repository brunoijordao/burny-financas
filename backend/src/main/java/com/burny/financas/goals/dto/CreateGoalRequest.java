package com.burny.financas.goals.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateGoalRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotNull(message = "Target amount is required")
        @Positive(message = "Target amount must be greater than zero")
        BigDecimal targetAmount,

        @NotNull(message = "Deadline is required")
        @Future(message = "Deadline must be in the future")
        LocalDate deadline
) {
}
