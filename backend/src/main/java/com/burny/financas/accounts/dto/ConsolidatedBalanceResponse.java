package com.burny.financas.accounts.dto;

import java.math.BigDecimal;

public record ConsolidatedBalanceResponse(
        BigDecimal consolidatedBalance
) {
}
