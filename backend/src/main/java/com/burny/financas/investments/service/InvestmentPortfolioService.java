package com.burny.financas.investments.service;

import com.burny.financas.investments.dto.AllocationItem;
import com.burny.financas.investments.dto.BenchmarkComparisonResponse;
import com.burny.financas.investments.dto.BenchmarkType;
import com.burny.financas.investments.dto.NetWorthEvolutionPoint;
import com.burny.financas.investments.dto.PortfolioSummaryResponse;
import com.burny.financas.investments.entity.AssetType;
import com.burny.financas.investments.entity.InvestmentAsset;
import com.burny.financas.investments.entity.InvestmentValuation;
import com.burny.financas.investments.repository.InvestmentAssetRepository;
import com.burny.financas.investments.repository.InvestmentOperationRepository;
import com.burny.financas.investments.repository.InvestmentValuationRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consolidated, read-only views over a user's assets. Unlike {@link InvestmentAssetService}'s
 * per-asset response (which reports {@code null} current value when an asset has no valuation
 * yet), every portfolio-level figure here falls back to an unvalued asset's invested amount so it
 * contributes zero profitability rather than distorting the total (design.md Decision 2, spec.md
 * "Unvalued asset does not distort portfolio profitability"). Benchmark comparison is a stateless
 * computation — nothing is persisted (design.md Decision 3).
 */
@Service
@RequiredArgsConstructor
public class InvestmentPortfolioService {

    private final InvestmentAssetRepository investmentAssetRepository;
    private final InvestmentOperationRepository investmentOperationRepository;
    private final InvestmentValuationRepository investmentValuationRepository;
    private final InvestmentPositionService investmentPositionService;

    @Transactional(readOnly = true)
    public PortfolioSummaryResponse getPortfolioSummary(Long userId) {
        List<InvestmentAsset> assets = investmentAssetRepository.findAllByUserIdAndActiveTrueOrderByNameAsc(userId);

        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalCurrentValue = BigDecimal.ZERO;
        for (InvestmentAsset asset : assets) {
            AssetPosition position = position(asset.getId());
            totalInvested = totalInvested.add(position.investedAmount());
            totalCurrentValue = totalCurrentValue.add(currentValueWithFallback(asset, position));
        }

        BigDecimal profitabilityAmount = totalCurrentValue.subtract(totalInvested);
        BigDecimal profitabilityPercentage = totalInvested.signum() > 0
                ? profitabilityAmount.multiply(BigDecimal.valueOf(100)).divide(totalInvested, 2, RoundingMode.HALF_UP)
                : null;

        return new PortfolioSummaryResponse(totalInvested, totalCurrentValue, profitabilityAmount, profitabilityPercentage);
    }

    @Transactional(readOnly = true)
    public List<AllocationItem> getAllocationByType(Long userId) {
        List<InvestmentAsset> assets = investmentAssetRepository.findAllByUserIdAndActiveTrueOrderByNameAsc(userId);

        Map<AssetType, BigDecimal> totalsByType = new EnumMap<>(AssetType.class);
        BigDecimal grandTotal = BigDecimal.ZERO;
        for (InvestmentAsset asset : assets) {
            BigDecimal currentValue = currentValueWithFallback(asset, position(asset.getId()));
            totalsByType.merge(asset.getType(), currentValue, BigDecimal::add);
            grandTotal = grandTotal.add(currentValue);
        }

        BigDecimal total = grandTotal;
        return totalsByType.entrySet().stream()
                .map(entry -> new AllocationItem(
                        entry.getKey(),
                        entry.getValue(),
                        total.signum() > 0
                                ? entry.getValue().multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO))
                .toList();
    }

    @Transactional(readOnly = true)
    public BenchmarkComparisonResponse getBenchmarkComparison(
            Long userId, BenchmarkType benchmarkType, BigDecimal benchmarkPercentage, LocalDate periodStart, LocalDate periodEnd) {
        List<InvestmentAsset> assets = investmentAssetRepository.findAllByUserIdAndActiveTrueOrderByNameAsc(userId);
        Map<Long, List<InvestmentValuation>> valuationsByAsset = valuationsByAsset(assets);

        BigDecimal startTotal = totalAsOf(assets, valuationsByAsset, periodStart);
        BigDecimal endTotal = totalAsOf(assets, valuationsByAsset, periodEnd);

        BigDecimal portfolioReturnPercentage = startTotal.signum() > 0
                ? endTotal.subtract(startTotal).multiply(BigDecimal.valueOf(100)).divide(startTotal, 2, RoundingMode.HALF_UP)
                : null;

        return new BenchmarkComparisonResponse(benchmarkType, benchmarkPercentage, portfolioReturnPercentage, periodStart, periodEnd);
    }

    @Transactional(readOnly = true)
    public List<NetWorthEvolutionPoint> getNetWorthEvolution(Long userId) {
        List<InvestmentAsset> assets = investmentAssetRepository.findAllByUserIdAndActiveTrueOrderByNameAsc(userId);
        if (assets.isEmpty()) {
            return List.of();
        }
        Map<Long, List<InvestmentValuation>> valuationsByAsset = valuationsByAsset(assets);

        SortedSet<LocalDate> dates = new TreeSet<>();
        valuationsByAsset.values().forEach(list -> list.forEach(v -> dates.add(v.getValueDate())));

        return dates.stream()
                .map(date -> new NetWorthEvolutionPoint(date, totalAsOf(assets, valuationsByAsset, date)))
                .toList();
    }

    /** Carries each asset's most recent valuation forward as of {@code date}; falls back to its invested amount if it has none yet. */
    private BigDecimal totalAsOf(List<InvestmentAsset> assets, Map<Long, List<InvestmentValuation>> valuationsByAsset, LocalDate date) {
        BigDecimal total = BigDecimal.ZERO;
        for (InvestmentAsset asset : assets) {
            List<InvestmentValuation> valuations = valuationsByAsset.getOrDefault(asset.getId(), List.of());
            BigDecimal valueAsOf = valuations.stream()
                    .filter(v -> !v.getValueDate().isAfter(date))
                    .max(Comparator.comparing(InvestmentValuation::getValueDate).thenComparing(InvestmentValuation::getId))
                    .map(InvestmentValuation::getTotalValue)
                    .orElseGet(() -> position(asset.getId()).investedAmount());
            total = total.add(valueAsOf);
        }
        return total;
    }

    private Map<Long, List<InvestmentValuation>> valuationsByAsset(List<InvestmentAsset> assets) {
        List<Long> assetIds = assets.stream().map(InvestmentAsset::getId).toList();
        if (assetIds.isEmpty()) {
            return Map.of();
        }
        return investmentValuationRepository.findAllByAssetIdInAndActiveTrueOrderByValueDateAsc(assetIds).stream()
                .collect(Collectors.groupingBy(v -> v.getAsset().getId()));
    }

    private BigDecimal currentValueWithFallback(InvestmentAsset asset, AssetPosition position) {
        return investmentValuationRepository.findFirstByAssetIdAndActiveTrueOrderByValueDateDescIdDesc(asset.getId())
                .map(InvestmentValuation::getTotalValue)
                .orElse(position.investedAmount());
    }

    private AssetPosition position(Long assetId) {
        return investmentPositionService.calculate(
                investmentOperationRepository.findAllByAssetIdAndActiveTrueOrderByOperationDateAsc(assetId));
    }
}
