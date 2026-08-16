import { Badge } from '@/components/ui/badge'
import type { PlanningEntry } from '@/features/planning/api/planningApi'
import { getPlanningUrgency } from '@/features/planning/lib/urgency'

const labels: Record<ReturnType<typeof getPlanningUrgency>, string> = {
  settled: 'Baixado',
  overdue: 'Vencido',
  'near-due': 'Vence em breve',
  normal: 'Pendente',
}

/** Reuses the shared Badge component — no new color introduced beyond --primary/--destructive/--secondary. */
export function PlanningStatusBadge({ entry }: { entry: Pick<PlanningEntry, 'status' | 'dueDate'> }) {
  const urgency = getPlanningUrgency(entry)

  if (urgency === 'overdue') {
    return <Badge variant="destructive" className="bg-destructive text-destructive-foreground">{labels[urgency]}</Badge>
  }
  if (urgency === 'near-due') {
    return <Badge variant="destructive">{labels[urgency]}</Badge>
  }
  if (urgency === 'settled') {
    return <Badge variant="outline">{labels[urgency]}</Badge>
  }
  return <Badge variant="soft">{labels[urgency]}</Badge>
}
