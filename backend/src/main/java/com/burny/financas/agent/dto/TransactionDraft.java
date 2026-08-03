package com.burny.financas.agent.dto;

import com.burny.financas.transactions.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Returned by the {@code proposeTransaction} tool for the user to review; never creates a
 * transaction by itself (design.md Decision 4). {@code accountName}/{@code categoryName} are
 * included so the frontend confirmation card doesn't need a second lookup.
 */
public record TransactionDraft(
        Long accountId,
        String accountName,
        TransactionType type,
        BigDecimal amount,
        String description,
        Long categoryId,
        String categoryName,
        LocalDate date
) {
}
