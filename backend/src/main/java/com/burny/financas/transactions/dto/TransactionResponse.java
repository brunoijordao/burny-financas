package com.burny.financas.transactions.dto;

import com.burny.financas.transactions.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        String description,
        BigDecimal amount,
        TransactionType type,
        LocalDate transactionDate,
        Long accountId,
        Long categoryId,
        String note,
        Long recurrenceId,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
