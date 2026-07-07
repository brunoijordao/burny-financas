package com.burny.financas.transactions.dto;

import com.burny.financas.transactions.entity.RecurrenceFrequency;
import com.burny.financas.transactions.entity.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * {@code categoryId} is intentionally optional: when omitted, the service attempts automatic
 * category resolution from {@code description} (see "Automatic Category Suggestion") instead of
 * requiring the caller to always pick one.
 *
 * <p>{@code recurring}/{@code frequency}/{@code startDate}/{@code endDate} configure an optional
 * recurrence; when {@code recurring} is {@code true}, {@code frequency} and {@code startDate} are
 * required (validated in the service, since they're conditionally required and not expressible as
 * simple per-field Bean Validation annotations).
 */
public record CreateTransactionRequest(
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
        String note,

        boolean recurring,
        RecurrenceFrequency frequency,
        LocalDate startDate,
        LocalDate endDate
) {
}
