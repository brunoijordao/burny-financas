package com.burny.financas.planning.mapper;

import com.burny.financas.planning.dto.PlanningEntryResponse;
import com.burny.financas.planning.dto.PlanningEntryStatusView;
import com.burny.financas.planning.entity.PlanningEntry;
import com.burny.financas.planning.entity.PlanningEntryStatus;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PlanningEntryMapper {

    default PlanningEntryResponse toResponse(PlanningEntry entry) {
        return new PlanningEntryResponse(
                entry.getId(),
                entry.getType(),
                entry.getAccount().getId(),
                entry.getAccount().getName(),
                entry.getCategory() != null ? entry.getCategory().getId() : null,
                entry.getCategory() != null ? entry.getCategory().getName() : null,
                entry.getAmount(),
                entry.getDescription(),
                entry.getDueDate(),
                toStatusView(entry),
                entry.getSettledAt(),
                entry.getTransaction() != null ? entry.getTransaction().getId() : null,
                entry.getCreatedAt(),
                entry.getUpdatedAt());
    }

    private PlanningEntryStatusView toStatusView(PlanningEntry entry) {
        if (entry.getStatus() == PlanningEntryStatus.SETTLED) {
            return PlanningEntryStatusView.SETTLED;
        }
        return entry.isOverdue() ? PlanningEntryStatusView.OVERDUE : PlanningEntryStatusView.PENDING;
    }
}
