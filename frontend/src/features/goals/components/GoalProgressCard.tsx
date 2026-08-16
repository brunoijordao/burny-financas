import { Check } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { cn } from '@/lib/utils'
import { formatCurrency, formatDate } from '@/lib/formatters'
import type { Goal } from '@/features/goals/api/goalsApi'
import { usePreferencesStore } from '@/features/settings/store/preferencesStore'

interface GoalProgressCardProps {
  goal: Goal
  onAddContribution: () => void
  onEdit: () => void
  onDelete: () => void
}

export function GoalProgressCard({ goal, onAddContribution, onEdit, onDelete }: GoalProgressCardProps) {
  const currency = usePreferencesStore((state) => state.currency)
  const dateFormat = usePreferencesStore((state) => state.dateFormat)
  const barWidth = Math.min(goal.percentComplete, 100)

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between gap-2 space-y-0">
        <CardTitle>{goal.name}</CardTitle>
        {goal.completed ? (
          <Badge variant="solid" className="gap-1">
            <Check className="size-3.5" />
            Concluída
          </Badge>
        ) : null}
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        <div>
          <p className="text-xs text-muted-foreground">
            {formatCurrency(goal.currentAmount, currency)} de {formatCurrency(goal.targetAmount, currency)}
          </p>
          <div className="mt-1 h-2 w-full overflow-hidden rounded-full bg-secondary">
            <div className="h-full rounded-full bg-primary transition-[width]" style={{ width: `${barWidth}%` }} />
          </div>
          <p className="mt-1 text-xs text-muted-foreground">{goal.percentComplete.toFixed(0)}% da meta</p>
        </div>

        <div className="flex flex-col gap-0.5 text-xs text-muted-foreground">
          <span>Prazo: {formatDate(goal.deadline, dateFormat)}</span>
          {goal.onTrack !== null ? (
            <span className={cn(goal.onTrack ? 'text-foreground' : 'text-destructive')}>
              {goal.onTrack
                ? `No ritmo atual, previsão de conclusão em ${formatDate(goal.projectedCompletionDate as string, dateFormat)}`
                : `No ritmo atual, a meta não será atingida até o prazo (previsão: ${formatDate(goal.projectedCompletionDate as string, dateFormat)})`}
            </span>
          ) : null}
        </div>

        <div className="flex flex-wrap justify-end gap-2">
          <Button variant="outline" size="sm" onClick={onAddContribution}>
            Registrar aporte
          </Button>
          <Button variant="outline" size="sm" onClick={onEdit}>
            Editar
          </Button>
          <Button variant="ghost" size="sm" onClick={onDelete}>
            Excluir
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
