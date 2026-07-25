import { cn } from '@/lib/utils'
import type { PlanningEntry } from '@/features/planning/api/planningApi'
import { getPlanningUrgency } from '@/features/planning/lib/urgency'

const labels: Record<ReturnType<typeof getPlanningUrgency>, string> = {
  settled: 'Baixado',
  overdue: 'Vencido',
  'near-due': 'Vence em breve',
  normal: 'Pendente',
}

/** Reuses --primary/--destructive/--secondary only — no new color introduced (mirrors BudgetProgressBar's approach). */
export function PlanningStatusBadge({ entry }: { entry: Pick<PlanningEntry, 'status' | 'dueDate'> }) {
  const urgency = getPlanningUrgency(entry)

  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium',
        urgency === 'settled' && 'bg-primary/15 text-primary',
        urgency === 'overdue' && 'bg-destructive text-destructive-foreground',
        urgency === 'near-due' && 'bg-destructive/15 text-destructive',
        urgency === 'normal' && 'bg-secondary text-secondary-foreground',
      )}
    >
      {labels[urgency]}
    </span>
  )
}
