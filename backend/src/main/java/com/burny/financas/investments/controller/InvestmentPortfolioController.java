package com.burny.financas.investments.controller;

import com.burny.financas.investments.dto.AllocationItem;
import com.burny.financas.investments.dto.BenchmarkComparisonResponse;
import com.burny.financas.investments.dto.BenchmarkType;
import com.burny.financas.investments.dto.NetWorthEvolutionPoint;
import com.burny.financas.investments.dto.PortfolioSummaryResponse;
import com.burny.financas.investments.service.InvestmentPortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Investment Portfolio", description = "Consolidated views across all of the authenticated user's investment assets: profitability, allocation by type, net worth evolution, and manual benchmark comparison")
@RestController
@RequestMapping("/investments/portfolio")
@RequiredArgsConstructor
public class InvestmentPortfolioController {

    private final InvestmentPortfolioService investmentPortfolioService;

    @Operation(summary = "Get the authenticated user's consolidated portfolio profitability (total invested, total current value, absolute and percentage return)")
    @GetMapping("/summary")
    public PortfolioSummaryResponse summary(Authentication authentication) {
        return investmentPortfolioService.getPortfolioSummary(currentUserId(authentication));
    }

    @Operation(summary = "Get the authenticated user's portfolio allocation, percentage of current value by asset type")
    @GetMapping("/allocation")
    public List<AllocationItem> allocation(Authentication authentication) {
        return investmentPortfolioService.getAllocationByType(currentUserId(authentication));
    }

    @Operation(summary = "Get the authenticated user's net worth evolution: portfolio total at each distinct date any asset has a recorded valuation")
    @GetMapping("/net-worth-evolution")
    public List<NetWorthEvolutionPoint> netWorthEvolution(Authentication authentication) {
        return investmentPortfolioService.getNetWorthEvolution(currentUserId(authentication));
    }

    @Operation(summary = "Compare the authenticated user's portfolio return for a period against a manually entered CDI/IBOVESPA/IPCA percentage — no external market data is used")
    @GetMapping("/benchmark-comparison")
    public BenchmarkComparisonResponse benchmarkComparison(
            @RequestParam BenchmarkType benchmark,
            @RequestParam BigDecimal percentage,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd,
            Authentication authentication
    ) {
        return investmentPortfolioService.getBenchmarkComparison(
                currentUserId(authentication), benchmark, percentage, periodStart, periodEnd);
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
