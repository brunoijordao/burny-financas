package com.burny.financas.pdfimport.dto;

import com.burny.financas.transactions.entity.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdatePdfImportItemRequest(
        @NotNull(message = "Transaction date is required")
        LocalDate transactionDate,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "Type is required")
        TransactionType type,

        Long categoryId
) {
}
