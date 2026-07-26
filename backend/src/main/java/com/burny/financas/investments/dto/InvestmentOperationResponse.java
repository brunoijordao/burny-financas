package com.burny.financas.investments.dto;

import com.burny.financas.investments.entity.OperationType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InvestmentOperationResponse(
        Long id,
        Long assetId,
        OperationType type,
        BigDecimal quantity,
        BigDecimal unitPrice,
        LocalDate operationDate,
        LocalDateTime createdAt
) {
}
