package com.burny.financas.reports.service;

import com.burny.financas.accounts.service.AccountService;
import com.burny.financas.investments.service.InvestmentPortfolioService;
import com.burny.financas.reports.dto.CategorySpendingDto;
import com.burny.financas.reports.dto.NetWorthEvolutionReportDto;
import com.burny.financas.reports.dto.ReportPeriod;
import com.burny.financas.reports.dto.StatementLineDto;
import com.burny.financas.reports.mapper.ReportMapper;
import com.burny.financas.transactions.entity.TransactionType;
import com.burny.financas.transactions.repository.TransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only aggregation/presentation layer over the caller's own data. Owns no data of its own —
 * every figure is produced by existing {@code transactions}/{@code accounts}/{@code investments}
 * repository or service methods, never re-derived here (design.md Decision 1).
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String UNCATEGORIZED_LABEL = "Sem categoria";

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final InvestmentPortfolioService investmentPortfolioService;
    private final ReportMapper reportMapper;

    @Transactional(readOnly = true)
    public List<StatementLineDto> getStatement(
            Long userId, LocalDate startDate, LocalDate endDate, Long accountId, Long categoryId, TransactionType type) {
        ReportPeriod period = ReportPeriod.of(startDate, endDate);
        return transactionRepository.findFiltered(
                        userId, true, accountId, categoryId, type, period.startDate(), period.endDate(), Pageable.unpaged())
                .map(reportMapper::toStatementLine)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategorySpendingDto> getSpendingByCategory(Long userId, LocalDate startDate, LocalDate endDate) {
        ReportPeriod period = ReportPeriod.of(startDate, endDate);

        List<TransactionRepository.CategoryExpenseProjection> groups = transactionRepository
                .sumExpenseByCategoryForDateRange(userId, true, TransactionType.EXPENSE, period.startDate(), period.endDate());

        BigDecimal grandTotal = groups.stream()
                .map(TransactionRepository.CategoryExpenseProjection::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return groups.stream()
                .map(projection -> new CategorySpendingDto(
                        projection.getCategoryId(),
                        projection.getCategoryId() == null ? UNCATEGORIZED_LABEL : projection.getCategoryName(),
                        projection.getIcon(),
                        projection.getColor(),
                        projection.getTotal(),
                        percentageOf(projection.getTotal(), grandTotal)))
                .toList();
    }

    @Transactional(readOnly = true)
    public NetWorthEvolutionReportDto getNetWorthEvolution(Long userId) {
        BigDecimal currentBalance = accountService.getConsolidatedBalance(userId).consolidatedBalance();
        return new NetWorthEvolutionReportDto(currentBalance, investmentPortfolioService.getNetWorthEvolution(userId));
    }

    private BigDecimal percentageOf(BigDecimal amount, BigDecimal total) {
        return total.signum() > 0
                ? amount.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }
}
