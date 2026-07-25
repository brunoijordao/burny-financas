package com.burny.financas.planning.dto;

import com.burny.financas.planning.entity.PlanningEntryType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PlanningEntryResponse(
        Long id,
        PlanningEntryType type,
        Long accountId,
        String accountName,
        Long categoryId,
        String categoryName,
        BigDecimal amount,
        String description,
        LocalDate dueDate,
        PlanningEntryStatusView status,
        LocalDateTime settledAt,
        Long transactionId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
