package com.burny.financas.goals.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ContributionResponse(
        Long id,
        Long goalId,
        BigDecimal amount,
        LocalDate contributionDate,
        LocalDateTime createdAt
) {
}
