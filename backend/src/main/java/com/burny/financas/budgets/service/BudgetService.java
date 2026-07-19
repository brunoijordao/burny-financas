package com.burny.financas.budgets.service;

import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.budgets.dto.BudgetResponse;
import com.burny.financas.budgets.entity.Budget;
import com.burny.financas.budgets.exception.BudgetNotFoundException;
import com.burny.financas.budgets.mapper.BudgetMapper;
import com.burny.financas.budgets.repository.BudgetRepository;
import com.burny.financas.categories.entity.Category;
import com.burny.financas.categories.exception.CategoryNotFoundException;
import com.burny.financas.categories.repository.CategoryRepository;
import com.burny.financas.transactions.entity.TransactionType;
import com.burny.financas.transactions.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spent amounts are never stored — every read recomputes them from {@link TransactionRepository}'s
 * {@code sumExpenseByCategoryForDateRange}, the same aggregation query {@code DashboardService}
 * uses for its category breakdown (see design.md Decision 1). Budgets own no aggregation logic of
 * their own.
 */
@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetMapper budgetMapper;

    /** Upsert: creates the current month's budget for the category if none exists, updates (and reactivates) it otherwise. */
    @Transactional
    public BudgetResponse setBudget(Long userId, Long categoryId, BigDecimal limitAmount) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));

        LocalDate currentMonth = currentMonthStart();
        Budget budget = budgetRepository.findByUserIdAndCategoryIdAndBudgetMonth(userId, categoryId, currentMonth)
                .orElseGet(() -> Budget.builder()
                        .user(userRepository.getReferenceById(userId))
                        .category(category)
                        .budgetMonth(currentMonth)
                        .build());

        budget.setLimitAmount(limitAmount);
        budget.setActive(true);
        Budget saved = budgetRepository.save(budget);

        return budgetMapper.toResponse(saved, spentAmountFor(userId, category.getId(), currentMonth));
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> list(Long userId) {
        LocalDate currentMonth = currentMonthStart();
        LocalDate monthEnd = currentMonth.plusMonths(1).minusDays(1);

        Map<Long, BigDecimal> spentByCategory = transactionRepository
                .sumExpenseByCategoryForDateRange(userId, true, TransactionType.EXPENSE, currentMonth, monthEnd)
                .stream()
                .filter(projection -> projection.getCategoryId() != null)
                .collect(Collectors.toMap(
                        TransactionRepository.CategoryExpenseProjection::getCategoryId,
                        TransactionRepository.CategoryExpenseProjection::getTotal));

        return budgetRepository.findByUserIdAndActiveAndBudgetMonth(userId, true, currentMonth).stream()
                .map(budget -> budgetMapper.toResponse(
                        budget,
                        spentByCategory.getOrDefault(budget.getCategory().getId(), BigDecimal.ZERO)))
                .toList();
    }

    @Transactional
    public void delete(Long userId, Long budgetId) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new BudgetNotFoundException("Budget not found"));
        budget.setActive(false);
        budgetRepository.save(budget);
    }

    private BigDecimal spentAmountFor(Long userId, Long categoryId, LocalDate monthStart) {
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        return transactionRepository
                .sumExpenseByCategoryForDateRange(userId, true, TransactionType.EXPENSE, monthStart, monthEnd)
                .stream()
                .filter(projection -> categoryId.equals(projection.getCategoryId()))
                .map(TransactionRepository.CategoryExpenseProjection::getTotal)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private static LocalDate currentMonthStart() {
        return LocalDate.now().withDayOfMonth(1);
    }
}
