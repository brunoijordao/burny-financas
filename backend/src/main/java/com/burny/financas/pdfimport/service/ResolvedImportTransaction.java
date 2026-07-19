package com.burny.financas.pdfimport.service;

import com.burny.financas.categories.entity.Category;
import com.burny.financas.transactions.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A Gemma-identified transaction after category resolution: either the model's suggestion matched
 * one of the user's own categories, or keyword-based fallback resolved one, or neither did and
 * {@code category} is {@code null} (see specs/pdf-statement-import "AI-Assisted Transaction
 * Interpretation With Category Fallback").
 */
public record ResolvedImportTransaction(
        LocalDate transactionDate,
        String description,
        BigDecimal amount,
        TransactionType type,
        Category category
) {
}
