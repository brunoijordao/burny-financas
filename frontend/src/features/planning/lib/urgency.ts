import type { PlanningEntryStatus } from '@/features/planning/api/planningApi'

export type PlanningUrgency = 'overdue' | 'near-due' | 'normal' | 'settled'

const DEFAULT_NEAR_DUE_DAYS = 3

/** Visual urgency bucket for calendar/list badges — computed client-side from the same fields the backend already returns. */
export function getPlanningUrgency(
  entry: { status: PlanningEntryStatus; dueDate: string },
  nearDueDays = DEFAULT_NEAR_DUE_DAYS,
): PlanningUrgency {
  if (entry.status === 'SETTLED') {
    return 'settled'
  }
  if (entry.status === 'OVERDUE') {
    return 'overdue'
  }

  const due = new Date(entry.dueDate + 'T00:00:00')
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const daysUntilDue = Math.round((due.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))

  return daysUntilDue <= nearDueDays ? 'near-due' : 'normal'
}
