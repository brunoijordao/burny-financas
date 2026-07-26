package com.burny.financas.investments.service;

import com.burny.financas.investments.entity.InvestmentOperation;
import com.burny.financas.investments.entity.OperationType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Derives a position from an asset's active operations using the average-cost method (design.md
 * Decision 1): a {@code BUY} adds to quantity and invested amount; a {@code SELL} reduces quantity
 * and reduces invested amount proportionally, leaving the average price unchanged.
 */
@Service
public class InvestmentPositionService {

    public AssetPosition calculate(List<InvestmentOperation> operations) {
        BigDecimal quantity = BigDecimal.ZERO;
        BigDecimal investedAmount = BigDecimal.ZERO;

        List<InvestmentOperation> ordered = operations.stream()
                .sorted(Comparator.comparing(InvestmentOperation::getOperationDate)
                        .thenComparing(InvestmentOperation::getId))
                .toList();

        for (InvestmentOperation operation : ordered) {
            if (operation.getType() == OperationType.BUY) {
                investedAmount = investedAmount.add(operation.getQuantity().multiply(operation.getUnitPrice()));
                quantity = quantity.add(operation.getQuantity());
            } else {
                BigDecimal averagePriceBeforeSell = quantity.signum() > 0
                        ? investedAmount.divide(quantity, 10, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                investedAmount = investedAmount.subtract(averagePriceBeforeSell.multiply(operation.getQuantity()));
                quantity = quantity.subtract(operation.getQuantity());
            }
        }

        investedAmount = investedAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal averagePrice = quantity.signum() > 0
                ? investedAmount.divide(quantity, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new AssetPosition(quantity, averagePrice, investedAmount);
    }
}
