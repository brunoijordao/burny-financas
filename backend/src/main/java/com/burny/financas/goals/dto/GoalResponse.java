package com.burny.financas.goals.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * {@code currentAmount}, {@code percentComplete}, {@code projectedCompletionDate}, and {@code
 * onTrack} are all computed per request from the contribution ledger (design.md Decision 3) —
 * none of them are persisted columns.
 */
public record GoalResponse(
        Long id,
        String name,
        BigDecimal targetAmount,
        LocalDate deadline,
        BigDecimal currentAmount,
        BigDecimal percentComplete,
        boolean completed,
        LocalDate projectedCompletionDate,
        Boolean onTrack,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
