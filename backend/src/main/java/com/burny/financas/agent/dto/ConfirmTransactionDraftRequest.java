package com.burny.financas.agent.dto;

import com.burny.financas.transactions.entity.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Submitted back by the frontend when the user explicitly confirms a draft shown by the agent.
 * Never trusted as-is (design.md Decision 5): {@code accountId}/{@code categoryId} are re-validated
 * against the caller's own data before {@code TransactionService.create} is called.
 */
public record ConfirmTransactionDraftRequest(
        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "Type is required")
        TransactionType type,

        LocalDate date,

        @NotNull(message = "Account is required")
        Long accountId,

        Long categoryId
) {
}
