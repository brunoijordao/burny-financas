package com.burny.financas.budgets.repository;

import com.burny.financas.budgets.entity.Budget;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByIdAndUserId(Long id, Long userId);

    Optional<Budget> findByUserIdAndCategoryIdAndBudgetMonth(Long userId, Long categoryId, LocalDate budgetMonth);

    @Query("""
            SELECT b FROM Budget b
            WHERE b.user.id = :userId
              AND b.active = :active
              AND b.budgetMonth = :budgetMonth
            """)
    List<Budget> findByUserIdAndActiveAndBudgetMonth(
            @Param("userId") Long userId,
            @Param("active") boolean active,
            @Param("budgetMonth") LocalDate budgetMonth
    );

    /**
     * One (user, category) pair per active budget in {@code previousMonth} that has no budget row
     * (active or not — a soft-deleted row still occupies the unique {@code (user, category, month)}
     * slot) yet for {@code currentMonth}. Drives {@link com.burny.financas.budgets.service.BudgetRenewalService}.
     */
    @Query("""
            SELECT b.user.id AS userId, b.category.id AS categoryId
            FROM Budget b
            WHERE b.active = :active
              AND b.budgetMonth = :previousMonth
              AND NOT EXISTS (
                  SELECT 1 FROM Budget b2
                  WHERE b2.user = b.user AND b2.category = b.category AND b2.budgetMonth = :currentMonth
              )
            """)
    List<RenewalCandidate> findRenewalCandidates(
            @Param("active") boolean active,
            @Param("previousMonth") LocalDate previousMonth,
            @Param("currentMonth") LocalDate currentMonth
    );

    interface RenewalCandidate {
        Long getUserId();

        Long getCategoryId();
    }
}
