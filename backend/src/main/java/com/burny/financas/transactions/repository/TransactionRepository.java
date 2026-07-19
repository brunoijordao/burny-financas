package com.burny.financas.transactions.repository;

import com.burny.financas.transactions.entity.Transaction;
import com.burny.financas.transactions.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    /** Used to clone account/category/amount/description when generating the next occurrence. */
    Optional<Transaction> findTopByRecurrenceIdOrderByTransactionDateDescIdDesc(Long recurrenceId);

    /**
     * Nullable-parameter filters (each an {@code OR :param IS NULL} guard). {@code active} is bound
     * as a parameter rather than written as a JPQL literal: a hand-written literal {@code true}
     * compiles to an actual SQL {@code true} keyword, which H2's Oracle-compatibility mode cannot
     * compare against the NUMBER(1)-backed {@code active} column (see the identical fix applied to
     * {@code CategoryKeywordRepository.findAllActiveByUserId}, where this was first discovered).
     */
    @Query("""
            SELECT t FROM Transaction t
            WHERE t.user.id = :userId
              AND t.active = :active
              AND (:accountId IS NULL OR t.account.id = :accountId)
              AND (:categoryId IS NULL OR t.category.id = :categoryId)
              AND (:type IS NULL OR t.type = :type)
              AND (:startDate IS NULL OR t.transactionDate >= :startDate)
              AND (:endDate IS NULL OR t.transactionDate <= :endDate)
            """)
    Page<Transaction> findFiltered(
            @Param("userId") Long userId,
            @Param("active") boolean active,
            @Param("accountId") Long accountId,
            @Param("categoryId") Long categoryId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    /** Used by the dashboard summary to sum a single month/type window without paging through results. */
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
            WHERE t.user.id = :userId
              AND t.active = :active
              AND t.type = :type
              AND t.transactionDate >= :startDate
              AND t.transactionDate <= :endDate
            """)
    BigDecimal sumAmountByTypeAndDateRange(
            @Param("userId") Long userId,
            @Param("active") boolean active,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Expense totals grouped by category for the dashboard's category breakdown. {@code LEFT JOIN}
     * (rather than an inner join through {@code t.category}) so uncategorized expenses (nullable
     * {@code category}) still form their own group instead of being dropped.
     */
    @Query("""
            SELECT c.id AS categoryId, c.name AS categoryName, c.icon AS icon, c.color AS color,
                   COALESCE(SUM(t.amount), 0) AS total
            FROM Transaction t
            LEFT JOIN t.category c
            WHERE t.user.id = :userId
              AND t.active = :active
              AND t.type = :type
              AND t.transactionDate >= :startDate
              AND t.transactionDate <= :endDate
            GROUP BY c.id, c.name, c.icon, c.color
            ORDER BY COALESCE(SUM(t.amount), 0) DESC
            """)
    List<CategoryExpenseProjection> sumExpenseByCategoryForDateRange(
            @Param("userId") Long userId,
            @Param("active") boolean active,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    interface CategoryExpenseProjection {
        Long getCategoryId();

        String getCategoryName();

        String getIcon();

        String getColor();

        BigDecimal getTotal();
    }
}
