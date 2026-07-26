package com.burny.financas.investments.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NetWorthEvolutionPoint(
        LocalDate date,
        BigDecimal totalValue
) {
}
