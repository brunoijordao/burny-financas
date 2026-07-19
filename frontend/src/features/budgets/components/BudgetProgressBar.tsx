import { cn } from '@/lib/utils'
import { computeBudgetProgress } from '@/features/budgets/lib/progress'

interface BudgetProgressBarProps {
  limitAmount: number | null
  spentAmount: number
}

/** Under 100% uses --primary, at/over 100% switches to --destructive — no new color introduced (design.md Decision 5). */
export function BudgetProgressBar({ limitAmount, spentAmount }: BudgetProgressBarProps) {
  if (limitAmount === null) {
    return <p className="text-sm text-muted-foreground">Limite não definido</p>
  }

  const { percent, barWidth, overLimit } = computeBudgetProgress(limitAmount, spentAmount)

  return (
    <div className="flex flex-col gap-1">
      <div className="h-2 w-full overflow-hidden rounded-full bg-secondary">
        <div
          className={cn('h-full rounded-full transition-[width]', overLimit ? 'bg-destructive' : 'bg-primary')}
          style={{ width: `${barWidth}%` }}
        />
      </div>
      <p className={cn('text-xs', overLimit ? 'text-destructive' : 'text-muted-foreground')}>
        {percent.toFixed(0)}% do limite{overLimit ? ' — orçamento ultrapassado' : ''}
      </p>
    </div>
  )
}
