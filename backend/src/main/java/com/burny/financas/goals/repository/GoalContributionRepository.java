package com.burny.financas.goals.repository;

import com.burny.financas.goals.entity.GoalContribution;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoalContributionRepository extends JpaRepository<GoalContribution, Long> {

    List<GoalContribution> findByGoalIdOrderByContributionDateDescIdDesc(Long goalId);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM GoalContribution c WHERE c.goal.id = :goalId")
    BigDecimal sumAmountByGoalId(@Param("goalId") Long goalId);
}
