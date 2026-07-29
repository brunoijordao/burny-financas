package com.burny.financas.reports.dto;

import com.burny.financas.investments.dto.NetWorthEvolutionPoint;
import java.math.BigDecimal;
import java.util.List;

/**
 * {@code currentConsolidatedAccountBalance} is a present-day snapshot ({@link
 * com.burny.financas.accounts.service.AccountService#getConsolidatedBalance}), not a historical
 * series — the system has no historical account-balance ledger (design.md Trade-off). {@code
 * investmentNetWorthEvolution} is the investments module's own true historical series, reused
 * verbatim, never recomputed.
 */
public record NetWorthEvolutionReportDto(
        BigDecimal currentConsolidatedAccountBalance,
        List<NetWorthEvolutionPoint> investmentNetWorthEvolution
) {
}
