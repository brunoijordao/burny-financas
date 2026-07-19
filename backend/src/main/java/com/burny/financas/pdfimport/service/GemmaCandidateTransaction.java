package com.burny.financas.pdfimport.service;

import com.burny.financas.transactions.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single transaction as identified by Gemma from the statement text, before category-keyword
 * fallback (see {@link PdfInterpretationService}). {@code suggestedCategoryName} is the model's raw
 * category guess (nullable) — free text, not yet resolved against the user's actual categories.
 */
public record GemmaCandidateTransaction(
        LocalDate transactionDate,
        String description,
        BigDecimal amount,
        TransactionType type,
        String suggestedCategoryName
) {
}
