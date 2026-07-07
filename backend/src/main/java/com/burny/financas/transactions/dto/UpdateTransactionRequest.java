package com.burny.financas.transactions.dto;

import com.burny.financas.transactions.entity.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Deliberately has no recurrence fields: editing an occurrence never affects the recurrence
 * configuration (see specs/transaction-recurrence/spec.md "Independent Occurrence Editing And
 * Deletion").
 */
public record UpdateTransactionRequest(
        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "Type is required")
        TransactionType type,

        @NotNull(message = "Transaction date is required")
        LocalDate transactionDate,

        @NotNull(message = "Account is required")
        Long accountId,

        Long categoryId,

        @Size(max = 1000, message = "Note must be at most 1000 characters")
        String note
) {
}
