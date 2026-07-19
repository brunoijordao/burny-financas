package com.burny.financas.dashboard.service;

import com.burny.financas.dashboard.dto.CategoryBreakdownItem;
import com.burny.financas.dashboard.dto.DashboardSummaryResponse;
import com.burny.financas.dashboard.dto.MonthlyTrendItem;
import com.burny.financas.transactions.entity.TransactionType;
import com.burny.financas.transactions.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only aggregation over the caller's own transactions for the dashboard. Owns no data of its
 * own — every figure is computed via {@link TransactionRepository} query methods, never by
 * re-deriving business logic that already lives in the accounts/transactions modules.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int TREND_MONTHS = 6;
    private static final String UNCATEGORIZED_LABEL = "Sem categoria";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(Long userId, LocalDate referenceDate) {
        LocalDate resolvedReferenceDate = referenceDate != null ? referenceDate : LocalDate.now();
        LocalDate monthStart = resolvedReferenceDate.withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        LocalDate futureStart = resolvedReferenceDate.plusDays(1);

        BigDecimal monthIncome = sum(userId, TransactionType.INCOME, monthStart, monthEnd);
        BigDecimal monthExpense = sum(userId, TransactionType.EXPENSE, monthStart, monthEnd);
        BigDecimal futureIncome = sum(userId, TransactionType.INCOME, futureStart, monthEnd);
        BigDecimal futureExpense = sum(userId, TransactionType.EXPENSE, futureStart, monthEnd);

        List<CategoryBreakdownItem> categoryBreakdown = transactionRepository
                .sumExpenseByCategoryForDateRange(userId, true, TransactionType.EXPENSE, monthStart, monthEnd)
                .stream()
                .map(projection -> new CategoryBreakdownItem(
                        projection.getCategoryId(),
                        projection.getCategoryId() == null ? UNCATEGORIZED_LABEL : projection.getCategoryName(),
                        projection.getIcon(),
                        projection.getColor(),
                        projection.getTotal()))
                .toList();

        return new DashboardSummaryResponse(
                monthStart.format(MONTH_FORMATTER),
                monthIncome,
                monthExpense,
                monthIncome.subtract(monthExpense),
                futureIncome,
                futureExpense,
                categoryBreakdown,
                buildMonthlyTrend(userId, monthStart));
    }

    private List<MonthlyTrendItem> buildMonthlyTrend(Long userId, LocalDate resolvedMonthStart) {
        List<MonthlyTrendItem> trend = new ArrayList<>();
        for (int monthsAgo = TREND_MONTHS - 1; monthsAgo >= 0; monthsAgo--) {
            LocalDate trendMonthStart = resolvedMonthStart.minusMonths(monthsAgo);
            LocalDate trendMonthEnd = trendMonthStart.plusMonths(1).minusDays(1);
            trend.add(new MonthlyTrendItem(
                    trendMonthStart.format(MONTH_FORMATTER),
                    sum(userId, TransactionType.INCOME, trendMonthStart, trendMonthEnd),
                    sum(userId, TransactionType.EXPENSE, trendMonthStart, trendMonthEnd)));
        }
        return trend;
    }

    private BigDecimal sum(Long userId, TransactionType type, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.sumAmountByTypeAndDateRange(userId, true, type, startDate, endDate);
    }
}
