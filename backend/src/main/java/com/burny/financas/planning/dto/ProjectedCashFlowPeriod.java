package com.burny.financas.planning.dto;

import java.math.BigDecimal;

/** One projected month: totals from still-pending entries due in it, and the running balance after applying them. */
public record ProjectedCashFlowPeriod(
        String month,
        BigDecimal totalReceivable,
        BigDecimal totalPayable,
        BigDecimal projectedBalance
) {
}
