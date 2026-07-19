package com.burny.financas.goals.service;

import com.burny.financas.auth.repository.UserRepository;
import com.burny.financas.goals.dto.ContributionResponse;
import com.burny.financas.goals.dto.CreateGoalRequest;
import com.burny.financas.goals.dto.GoalResponse;
import com.burny.financas.goals.dto.UpdateGoalRequest;
import com.burny.financas.goals.entity.Goal;
import com.burny.financas.goals.entity.GoalContribution;
import com.burny.financas.goals.exception.GoalNotFoundException;
import com.burny.financas.goals.mapper.GoalMapper;
import com.burny.financas.goals.repository.GoalContributionRepository;
import com.burny.financas.goals.repository.GoalRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Progress, percent-complete, and the pace projection are all computed per request from the
 * contribution ledger (design.md Decision 3) — {@code Goal} itself only stores {@code completed},
 * which is set once (when a contribution brings the total to or above the target) and never unset.
 */
@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final GoalContributionRepository goalContributionRepository;
    private final UserRepository userRepository;
    private final GoalMapper goalMapper;

    @Transactional
    public GoalResponse create(Long userId, CreateGoalRequest request) {
        Goal goal = Goal.builder()
                .user(userRepository.getReferenceById(userId))
                .name(request.name())
                .targetAmount(request.targetAmount())
                .deadline(request.deadline())
                .completed(false)
                .active(true)
                .build();
        return toResponse(goalRepository.save(goal));
    }

    @Transactional
    public GoalResponse update(Long userId, Long id, UpdateGoalRequest request) {
        Goal goal = findOwnedOrThrow(id, userId);
        goal.setName(request.name());
        goal.setTargetAmount(request.targetAmount());
        goal.setDeadline(request.deadline());
        // Monotonic: an edit can newly satisfy the target, but never un-completes an already-completed goal.
        goal.setCompleted(goal.isCompleted() || currentAmount(goal.getId()).compareTo(request.targetAmount()) >= 0);
        return toResponse(goalRepository.save(goal));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Goal goal = findOwnedOrThrow(id, userId);
        goal.setActive(false);
        goalRepository.save(goal);
    }

    @Transactional(readOnly = true)
    public GoalResponse get(Long userId, Long id) {
        return toResponse(findOwnedOrThrow(id, userId));
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> list(Long userId) {
        return goalRepository.findByUserIdAndActive(userId, true).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ContributionResponse addContribution(Long userId, Long goalId, BigDecimal amount, LocalDate contributionDate) {
        Goal goal = findOwnedOrThrow(goalId, userId);

        GoalContribution contribution = GoalContribution.builder()
                .goal(goal)
                .amount(amount)
                .contributionDate(contributionDate != null ? contributionDate : LocalDate.now())
                .build();
        GoalContribution saved = goalContributionRepository.save(contribution);

        if (!goal.isCompleted() && currentAmount(goalId).compareTo(goal.getTargetAmount()) >= 0) {
            goal.setCompleted(true);
            goalRepository.save(goal);
        }

        return goalMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ContributionResponse> listContributions(Long userId, Long goalId) {
        findOwnedOrThrow(goalId, userId);
        return goalContributionRepository.findByGoalIdOrderByContributionDateDescIdDesc(goalId).stream()
                .map(goalMapper::toResponse)
                .toList();
    }

    private GoalResponse toResponse(Goal goal) {
        BigDecimal current = currentAmount(goal.getId());
        BigDecimal percentComplete = current
                .multiply(BigDecimal.valueOf(100))
                .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP);

        LocalDate projectedCompletionDate = null;
        Boolean onTrack = null;
        if (goal.isCompleted()) {
            onTrack = true;
        } else if (current.compareTo(BigDecimal.ZERO) > 0) {
            long daysSinceCreation = Math.max(1, ChronoUnit.DAYS.between(goal.getCreatedAt().toLocalDate(), LocalDate.now()));
            BigDecimal dailyPace = current.divide(BigDecimal.valueOf(daysSinceCreation), 10, RoundingMode.HALF_UP);
            BigDecimal remaining = goal.getTargetAmount().subtract(current);
            long daysNeeded = remaining.divide(dailyPace, 0, RoundingMode.CEILING).longValueExact();
            projectedCompletionDate = LocalDate.now().plusDays(daysNeeded);
            onTrack = !projectedCompletionDate.isAfter(goal.getDeadline());
        }

        return goalMapper.toResponse(goal, current, percentComplete, projectedCompletionDate, onTrack);
    }

    private BigDecimal currentAmount(Long goalId) {
        return goalContributionRepository.sumAmountByGoalId(goalId);
    }

    private Goal findOwnedOrThrow(Long id, Long userId) {
        return goalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new GoalNotFoundException("Goal not found"));
    }
}
