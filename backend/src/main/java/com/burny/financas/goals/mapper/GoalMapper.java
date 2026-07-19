package com.burny.financas.goals.mapper;

import com.burny.financas.goals.dto.ContributionResponse;
import com.burny.financas.goals.dto.GoalResponse;
import com.burny.financas.goals.entity.Goal;
import com.burny.financas.goals.entity.GoalContribution;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GoalMapper {

    /** Every progress/projection field is computed per request (see {@code GoalService}), not a persisted column. */
    default GoalResponse toResponse(
            Goal goal,
            BigDecimal currentAmount,
            BigDecimal percentComplete,
            LocalDate projectedCompletionDate,
            Boolean onTrack
    ) {
        return new GoalResponse(
                goal.getId(),
                goal.getName(),
                goal.getTargetAmount(),
                goal.getDeadline(),
                currentAmount,
                percentComplete,
                goal.isCompleted(),
                projectedCompletionDate,
                onTrack,
                goal.getCreatedAt(),
                goal.getUpdatedAt()
        );
    }

    default ContributionResponse toResponse(GoalContribution contribution) {
        return new ContributionResponse(
                contribution.getId(),
                contribution.getGoal().getId(),
                contribution.getAmount(),
                contribution.getContributionDate(),
                contribution.getCreatedAt()
        );
    }
}
