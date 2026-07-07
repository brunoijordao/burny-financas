package com.burny.financas.accounts.dto;

import com.burny.financas.accounts.entity.AccountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * For a CREDIT_CARD account, {@code balance} is null and {@code creditLimit}/{@code currentInvoice}
 * are populated instead; for every other type it's the reverse (see "Individual Account Balance").
 */
public record AccountResponse(
        Long id,
        String name,
        String icon,
        String color,
        AccountType type,
        boolean active,
        BigDecimal balance,
        BigDecimal creditLimit,
        BigDecimal currentInvoice,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
