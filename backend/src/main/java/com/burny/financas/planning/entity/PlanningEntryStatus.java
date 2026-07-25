package com.burny.financas.planning.entity;

/**
 * {@code OVERDUE} is intentionally not a value here — it is derived at read time from {@code
 * PENDING} plus {@code dueDate} vs. today (see design.md Decision 1), never stored or updated by a
 * scheduled job.
 */
public enum PlanningEntryStatus {
    PENDING,
    SETTLED
}
