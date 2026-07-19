import { Check } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { cn } from '@/lib/utils'
import type { Goal } from '@/features/goals/api/goalsApi'

function formatCurrency(value: number) {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

function formatDate(value: string) {
  return new Date(value + 'T00:00:00').toLocaleDateString('pt-BR')
}

interface GoalProgressCardProps {
  goal: Goal
  onAddContribution: () => void
  onEdit: () => void
  onDelete: () => void
}

export function GoalProgressCard({ goal, onAddContribution, onEdit, onDelete }: GoalProgressCardProps) {
  const barWidth = Math.min(goal.percentComplete, 100)

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between gap-2 space-y-0">
        <CardTitle>{goal.name}</CardTitle>
        {goal.completed ? (
          <span className="flex items-center gap-1 rounded-full bg-primary/10 px-2 py-1 text-xs font-medium text-primary">
            <Check className="size-3.5" />
            Concluída
          </span>
        ) : null}
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        <div>
          <p className="text-xs text-muted-foreground">
            {formatCurrency(goal.currentAmount)} de {formatCurrency(goal.targetAmount)}
          </p>
          <div className="mt-1 h-2 w-full overflow-hidden rounded-full bg-secondary">
            <div className="h-full rounded-full bg-primary transition-[width]" style={{ width: `${barWidth}%` }} />
          </div>
          <p className="mt-1 text-xs text-muted-foreground">{goal.percentComplete.toFixed(0)}% da meta</p>
        </div>

        <div className="flex flex-col gap-0.5 text-xs text-muted-foreground">
          <span>Prazo: {formatDate(goal.deadline)}</span>
          {goal.onTrack !== null ? (
            <span className={cn(goal.onTrack ? 'text-emerald-600 dark:text-emerald-400' : 'text-destructive')}>
              {goal.onTrack
                ? `No ritmo atual, previsão de conclusão em ${formatDate(goal.projectedCompletionDate as string)}`
                : `No ritmo atual, a meta não será atingida até o prazo (previsão: ${formatDate(goal.projectedCompletionDate as string)})`}
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
