package com.burny.financas.reports.dto;

import com.burny.financas.transactions.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Report-safe projection of a {@code Transaction}, with account/category already resolved to
 * display names so the PDF/Excel exporters never touch a lazy-loaded entity association.
 */
public record StatementLineDto(
        LocalDate transactionDate,
        String accountName,
        String categoryName,
        TransactionType type,
        BigDecimal amount,
        String description
) {
}
