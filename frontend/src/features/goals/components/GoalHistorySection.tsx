import type { Goal } from '@/features/goals/api/goalsApi'
import { GoalProgressCard } from '@/features/goals/components/GoalProgressCard'

interface GoalHistorySectionProps {
  goals: Goal[]
  onAddContribution: (goal: Goal) => void
  onEdit: (goal: Goal) => void
  onDelete: (goal: Goal) => void
}

/** Completed goals stay visible in their own section rather than being hidden or removed (design.md). */
export function GoalHistorySection({ goals, onAddContribution, onEdit, onDelete }: GoalHistorySectionProps) {
  const inProgress = goals.filter((goal) => !goal.completed)
  const completed = goals.filter((goal) => goal.completed)

  const renderCard = (goal: Goal) => (
    <GoalProgressCard
      key={goal.id}
      goal={goal}
      onAddContribution={() => onAddContribution(goal)}
      onEdit={() => onEdit(goal)}
      onDelete={() => onDelete(goal)}
    />
  )

  if (goals.length === 0) {
    return <p className="text-sm text-muted-foreground">Nenhuma meta cadastrada ainda.</p>
  }

  return (
    <div className="flex flex-col gap-8">
      <div className="flex flex-col gap-4">
        <h2 className="text-lg font-semibold">Em andamento</h2>
        {inProgress.length === 0 ? (
          <p className="text-sm text-muted-foreground">Nenhuma meta em andamento.</p>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{inProgress.map(renderCard)}</div>
        )}
      </div>

      {completed.length > 0 ? (
        <div className="flex flex-col gap-4">
          <h2 className="text-lg font-semibold">Concluídas</h2>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{completed.map(renderCard)}</div>
        </div>
      ) : null}
    </div>
  )
}
