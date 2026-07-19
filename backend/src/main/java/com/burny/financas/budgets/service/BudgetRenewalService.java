package com.burny.financas.budgets.service;

import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.budgets.entity.Budget;
import com.burny.financas.budgets.repository.BudgetRepository;
import com.burny.financas.categories.repository.CategoryRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps a budgeted category from silently disappearing at month rollover: for every (user,
 * category) with an active budget last month and no row yet this month, inserts a blank
 * ({@code limitAmount = null}) one for the user to fill in — see design.md Decision 2. Mirrors
 * {@code TransactionRecurrenceService}'s daily-cron shape, staggered to a different minute so the
 * two jobs never contend.
 */
@Service
@RequiredArgsConstructor
public class BudgetRenewalService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Scheduled(cron = "0 15 3 * * *")
    public void renewDueBudgetsScheduled() {
        renewDueBudgets();
    }

    @Transactional
    public void renewDueBudgets() {
        LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate previousMonth = currentMonth.minusMonths(1);

        for (BudgetRepository.RenewalCandidate candidate : budgetRepository.findRenewalCandidates(true, previousMonth, currentMonth)) {
            Budget budget = Budget.builder()
                    .user(userRepository.getReferenceById(candidate.getUserId()))
                    .category(categoryRepository.getReferenceById(candidate.getCategoryId()))
                    .budgetMonth(currentMonth)
                    .limitAmount(null)
                    .active(true)
                    .build();
            budgetRepository.save(budget);
        }
    }
}
