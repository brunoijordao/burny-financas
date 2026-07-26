package com.burny.financas.investments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.burny.financas.investments.dto.AllocationItem;
import com.burny.financas.investments.dto.BenchmarkComparisonResponse;
import com.burny.financas.investments.dto.BenchmarkType;
import com.burny.financas.investments.dto.NetWorthEvolutionPoint;
import com.burny.financas.investments.dto.PortfolioSummaryResponse;
import com.burny.financas.investments.entity.AssetType;
import com.burny.financas.investments.entity.InvestmentAsset;
import com.burny.financas.investments.entity.InvestmentOperation;
import com.burny.financas.investments.entity.InvestmentValuation;
import com.burny.financas.investments.entity.OperationType;
import com.burny.financas.investments.repository.InvestmentAssetRepository;
import com.burny.financas.investments.repository.InvestmentOperationRepository;
import com.burny.financas.investments.repository.InvestmentValuationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvestmentPortfolioServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private InvestmentAssetRepository investmentAssetRepository;
    @Mock
    private InvestmentOperationRepository investmentOperationRepository;
    @Mock
    private InvestmentValuationRepository investmentValuationRepository;

    private InvestmentPortfolioService service() {
        return new InvestmentPortfolioService(
                investmentAssetRepository, investmentOperationRepository, investmentValuationRepository,
                new InvestmentPositionService());
    }

    private InvestmentAsset asset(Long id, AssetType type) {
        return InvestmentAsset.builder().id(id).name("Asset " + id).type(type).active(true).build();
    }

    private InvestmentOperation buy(String quantity, String unitPrice, LocalDate date) {
        return InvestmentOperation.builder()
                .id(1L).type(OperationType.BUY).quantity(new BigDecimal(quantity))
                .unitPrice(new BigDecimal(unitPrice)).operationDate(date).active(true).build();
    }

    private InvestmentValuation valuation(Long id, InvestmentAsset asset, LocalDate date, String value) {
        return InvestmentValuation.builder()
                .id(id).asset(asset).valueDate(date).totalValue(new BigDecimal(value)).active(true).build();
    }

    @Test
    void portfolioProfitabilityAcrossMultipleAssets() {
        InvestmentAsset assetA = asset(1L, AssetType.STOCK);
        InvestmentAsset assetB = asset(2L, AssetType.FII);
        when(investmentAssetRepository.findAllByUserIdAndActiveTrueOrderByNameAsc(USER_ID))
                .thenReturn(List.of(assetA, assetB));
        when(investmentOperationRepository.findAllByAssetIdAndActiveTrueOrderByOperationDateAsc(1L))
                .thenReturn(List.of(buy("10", "100", LocalDate.now().minusDays(10))));
        when(investmentOperationRepository.findAllByAssetIdAndActiveTrueOrderByOperationDateAsc(2L))
                .thenReturn(List.of(buy("5", "100", LocalDate.now().minusDays(10))));
        when(investmentValuationRepository.findFirstByAssetIdAndActiveTrueOrderByValueDateDescIdDesc(1L))
                .thenReturn(java.util.Optional.of(valuation(1L, assetA, LocalDate.now(), "1200")));
        when(investmentValuationRepository.findFirstByAssetIdAndActiveTrueOrderByValueDateDescIdDesc(2L))
                .thenReturn(java.util.Optional.of(valuation(2L, assetB, LocalDate.now(), "400")));

        PortfolioSummaryResponse response = service().getPortfolioSummary(USER_ID);

        assertThat(response.totalInvested()).isEqualByComparingTo("1500");
        assertThat(response.totalCurrentValue()).isEqualByComparingTo("1600");
        assertThat(response.profitabilityAmount()).isEqualByComparingTo("100");
    }

    @Test
    void unvaluedAssetContributesZeroProfitabilityToPortfolioTotal() {
        InvestmentAsset assetA = asset(1L, AssetType.STOCK);
        when(investmentAssetRepository.findAllByUserIdAndActiveTrueOrderByNameAsc(USER_ID))
                .thenReturn(List.of(assetA));
        when(investmentOperationRepository.findAllByAssetIdAndActiveTrueOrderByOperationDateAsc(1L))
                .thenReturn(List.of(buy("3", "100", LocalDate.now().minusDays(10))));
        when(investmentValuationRepository.findFirstByAssetIdAndActiveTrueOrderByValueDateDescIdDesc(1L))
                .thenReturn(java.util.Optional.empty());

        PortfolioSummaryResponse response = service().getPortfolioSummary(USER_ID);

        assertThat(response.totalInvested()).isEqualByComparingTo("300");
        assertThat(response.totalCurrentValue()).isEqualByComparingTo("300");
        assertThat(response.profitabilityAmount()).isEqualByComparingTo("0");
    }

    @Test
    void allocationAcrossTwoTypes() {
        InvestmentAsset assetA = asset(1L, AssetType.STOCK);
        InvestmentAsset assetB = asset(2L, AssetType.FII);
        when(investmentAssetRepository.findAllByUserIdAndActiveTrueOrderByNameAsc(USER_ID))
                .thenReturn(List.of(assetA, assetB));
        when(investmentOperationRepository.findAllByAssetIdAndActiveTrueOrderByOperationDateAsc(1L))
                .thenReturn(List.of(buy("6", "100", LocalDate.now().minusDays(10))));
        when(investmentOperationRepository.findAllByAssetIdAndActiveTrueOrderByOperationDateAsc(2L))
                .thenReturn(List.of(buy("4", "100", LocalDate.now().minusDays(10))));
        when(investmentValuationRepository.findFirstByAssetIdAndActiveTrueOrderByValueDateDescIdDesc(1L))
                .thenReturn(java.util.Optional.of(valuation(1L, assetA, LocalDate.now(), "600")));
        when(investmentValuationRepository.findFirstByAssetIdAndActiveTrueOrderByValueDateDescIdDesc(2L))
                .thenReturn(java.util.Optional.of(valuation(2L, assetB, LocalDate.now(), "400")));

        List<AllocationItem> allocation = service().getAllocationByType(USER_ID);

        assertThat(allocation).hasSize(2);
        assertThat(allocation.stream().filter(i -> i.type() == AssetType.STOCK).findFirst().orElseThrow().percentage())
                .isEqualByComparingTo("60.00");
        assertThat(allocation.stream().filter(i -> i.type() == AssetType.FII).findFirst().orElseThrow().percentage())
                .isEqualByComparingTo("40.00");
    }

    @Test
    void netWorthEvolutionCarriesForwardEachAssetsLatestValuation() {
        InvestmentAsset assetA = asset(1L, AssetType.STOCK);
        InvestmentAsset assetB = asset(2L, AssetType.FII);
        LocalDate jan1 = LocalDate.of(2026, 1, 1);
        LocalDate feb1 = LocalDate.of(2026, 2, 1);

        when(investmentAssetRepository.findAllByUserIdAndActiveTrueOrderByNameAsc(USER_ID))
                .thenReturn(List.of(assetA, assetB));
        when(investmentOperationRepository.findAllByAssetIdAndActiveTrueOrderByOperationDateAsc(any()))
                .thenReturn(List.of());
        when(investmentValuationRepository.findAllByAssetIdInAndActiveTrueOrderByValueDateAsc(anyList()))
                .thenReturn(List.of(
                        valuation(1L, assetA, jan1, "500"),
                        valuation(2L, assetB, feb1, "300")));

        List<NetWorthEvolutionPoint> evolution = service().getNetWorthEvolution(USER_ID);

        assertThat(evolution).hasSize(2);
        assertThat(evolution.get(0).date()).isEqualTo(jan1);
        assertThat(evolution.get(0).totalValue()).isEqualByComparingTo("500");
        assertThat(evolution.get(1).date()).isEqualTo(feb1);
        assertThat(evolution.get(1).totalValue()).isEqualByComparingTo("800");
    }

    @Test
    void netWorthEvolutionFallsBackToInvestedAmountForUnvaluedAsset() {
        InvestmentAsset assetA = asset(1L, AssetType.STOCK);
        InvestmentAsset assetB = asset(2L, AssetType.FII);
        LocalDate jan1 = LocalDate.of(2026, 1, 1);

        when(investmentAssetRepository.findAllByUserIdAndActiveTrueOrderByNameAsc(USER_ID))
                .thenReturn(List.of(assetA, assetB));
        when(investmentOperationRepository.findAllByAssetIdAndActiveTrueOrderByOperationDateAsc(2L))
                .thenReturn(List.of(buy("2", "150", jan1.minusDays(5))));
        when(investmentValuationRepository.findAllByAssetIdInAndActiveTrueOrderByValueDateAsc(anyList()))
                .thenReturn(List.of(valuation(1L, assetA, jan1, "500")));

        List<NetWorthEvolutionPoint> evolution = service().getNetWorthEvolution(USER_ID);

        assertThat(evolution).hasSize(1);
        // 500 (assetA's valuation) + 300 (assetB's invested-amount fallback, no valuation yet)
        assertThat(evolution.get(0).totalValue()).isEqualByComparingTo("800");
    }

    @Test
    void benchmarkComparisonEchoesEnteredPercentageAlongsidePortfolioReturn() {
        InvestmentAsset assetA = asset(1L, AssetType.STOCK);
        LocalDate periodStart = LocalDate.of(2026, 1, 1);
        LocalDate periodEnd = LocalDate.of(2026, 2, 1);

        when(investmentAssetRepository.findAllByUserIdAndActiveTrueOrderByNameAsc(USER_ID))
                .thenReturn(List.of(assetA));
        when(investmentValuationRepository.findAllByAssetIdInAndActiveTrueOrderByValueDateAsc(anyList()))
                .thenReturn(List.of(
                        valuation(1L, assetA, periodStart, "1000"),
                        valuation(2L, assetA, periodEnd, "1100")));

        BenchmarkComparisonResponse response = service().getBenchmarkComparison(
                USER_ID, BenchmarkType.CDI, new BigDecimal("5.00"), periodStart, periodEnd);

        assertThat(response.benchmarkType()).isEqualTo(BenchmarkType.CDI);
        assertThat(response.benchmarkPercentage()).isEqualByComparingTo("5.00");
        assertThat(response.portfolioReturnPercentage()).isEqualByComparingTo("10.00");
    }
}
