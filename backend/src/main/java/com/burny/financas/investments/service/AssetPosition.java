package com.burny.financas.investments.service;

import java.math.BigDecimal;

/** Quantity held, weighted average buy price, and total invested amount, derived from operations — never stored (design.md Decision 1). */
public record AssetPosition(BigDecimal quantity, BigDecimal averagePrice, BigDecimal investedAmount) {
}
