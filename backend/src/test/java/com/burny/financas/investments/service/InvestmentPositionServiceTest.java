package com.burny.financas.investments.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.burny.financas.investments.entity.InvestmentOperation;
import com.burny.financas.investments.entity.OperationType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class InvestmentPositionServiceTest {

    private final InvestmentPositionService service = new InvestmentPositionService();

    private InvestmentOperation op(Long id, OperationType type, String quantity, String unitPrice, LocalDate date) {
        return InvestmentOperation.builder()
                .id(id)
                .type(type)
                .quantity(new BigDecimal(quantity))
                .unitPrice(new BigDecimal(unitPrice))
                .operationDate(date)
                .active(true)
                .build();
    }

    @Test
    void positionAfterSingleBuy() {
        AssetPosition position = service.calculate(List.of(
                op(1L, OperationType.BUY, "10", "50", LocalDate.now())));

        assertThat(position.quantity()).isEqualByComparingTo("10");
        assertThat(position.averagePrice()).isEqualByComparingTo("50");
        assertThat(position.investedAmount()).isEqualByComparingTo("500");
    }

    @Test
    void averagePriceUnchangedBySell() {
        AssetPosition position = service.calculate(List.of(
                op(1L, OperationType.BUY, "10", "50", LocalDate.now().minusDays(1)),
                op(2L, OperationType.SELL, "4", "999", LocalDate.now())));

        assertThat(position.quantity()).isEqualByComparingTo("6");
        assertThat(position.averagePrice()).isEqualByComparingTo("50");
        assertThat(position.investedAmount()).isEqualByComparingTo("300");
    }

    @Test
    void weightedAverageAcrossMultipleBuysAtDifferentPrices() {
        AssetPosition position = service.calculate(List.of(
                op(1L, OperationType.BUY, "10", "50", LocalDate.now().minusDays(1)),
                op(2L, OperationType.BUY, "10", "70", LocalDate.now())));

        assertThat(position.quantity()).isEqualByComparingTo("20");
        assertThat(position.averagePrice()).isEqualByComparingTo("60");
        assertThat(position.investedAmount()).isEqualByComparingTo("1200");
    }

    @Test
    void fullySoldAssetHasZeroQuantityAndInvestedAmount() {
        AssetPosition position = service.calculate(List.of(
                op(1L, OperationType.BUY, "10", "50", LocalDate.now().minusDays(1)),
                op(2L, OperationType.SELL, "10", "60", LocalDate.now())));

        assertThat(position.quantity()).isEqualByComparingTo("0");
        assertThat(position.investedAmount()).isEqualByComparingTo("0");
        assertThat(position.averagePrice()).isEqualByComparingTo("0");
    }

    @Test
    void noOperationsYieldsZeroPosition() {
        AssetPosition position = service.calculate(List.of());

        assertThat(position.quantity()).isEqualByComparingTo("0");
        assertThat(position.averagePrice()).isEqualByComparingTo("0");
        assertThat(position.investedAmount()).isEqualByComparingTo("0");
    }
}
