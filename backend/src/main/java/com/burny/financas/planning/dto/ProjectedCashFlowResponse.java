package com.burny.financas.planning.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProjectedCashFlowResponse(
        BigDecimal currentAvailableBalance,
        List<ProjectedCashFlowPeriod> periods
) {
}
