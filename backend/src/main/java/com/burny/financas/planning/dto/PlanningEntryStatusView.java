package com.burny.financas.planning.dto;

/** API-facing status, unlike the persisted {@code PlanningEntryStatus}: {@code OVERDUE} is computed, never stored. */
public enum PlanningEntryStatusView {
    PENDING,
    OVERDUE,
    SETTLED
}
