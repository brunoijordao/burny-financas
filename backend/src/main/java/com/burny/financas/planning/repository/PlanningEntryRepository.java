package com.burny.financas.planning.repository;

import com.burny.financas.planning.entity.PlanningEntry;
import com.burny.financas.planning.entity.PlanningEntryStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Every filter here is expressed as a Spring Data derived query (e.g. the {@code ActiveTrue}
 * suffix), which binds its boolean as a parameter rather than inlining a JPQL literal — no
 * hand-written {@code @Query} was needed, sidestepping the H2/Oracle boolean-literal
 * incompatibility documented on {@code CategoryKeywordRepository.findAllActiveByUserId}.
 */
public interface PlanningEntryRepository extends JpaRepository<PlanningEntry, Long> {

    Optional<PlanningEntry> findByIdAndUserId(Long id, Long userId);

    List<PlanningEntry> findAllByUserIdAndActiveTrueOrderByDueDateAsc(Long userId);

    /** Calendar view: every active entry (any status) due within the requested month. */
    List<PlanningEntry> findAllByUserIdAndActiveTrueAndDueDateBetweenOrderByDueDateAsc(
            Long userId, LocalDate startDate, LocalDate endDate);

    /** Projected cash flow: active, still-pending entries due within the requested window. */
    List<PlanningEntry> findAllByUserIdAndActiveTrueAndStatusAndDueDateBetweenOrderByDueDateAsc(
            Long userId, PlanningEntryStatus status, LocalDate startDate, LocalDate endDate);
}
