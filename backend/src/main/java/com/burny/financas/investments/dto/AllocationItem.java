package com.burny.financas.investments.dto;

import com.burny.financas.investments.entity.AssetType;
import java.math.BigDecimal;

public record AllocationItem(
        AssetType type,
        BigDecimal currentValue,
        BigDecimal percentage
) {
}
