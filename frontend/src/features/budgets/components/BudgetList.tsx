import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { CategoryIcon } from '@/features/categories/icons'
import type { Budget } from '@/features/budgets/api/budgetsApi'
import { BudgetProgressBar } from '@/features/budgets/components/BudgetProgressBar'

function formatCurrency(value: number) {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

interface BudgetListProps {
  budgets: Budget[]
  onEdit: (budget: Budget) => void
  onDelete: (budget: Budget) => void
}

export function BudgetList({ budgets, onEdit, onDelete }: BudgetListProps) {
  if (budgets.length === 0) {
    return <p className="text-sm text-muted-foreground">Nenhum orçamento definido para este mês ainda.</p>
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {budgets.map((budget) => (
        <Card key={budget.id}>
          <CardContent className="flex flex-col gap-3 pt-6">
            <div className="flex items-center gap-3">
              <span
                className="flex size-9 shrink-0 items-center justify-center rounded-full"
                style={{ backgroundColor: budget.color + '33', color: budget.color }}
              >
                <CategoryIcon icon={budget.icon} className="size-5" />
              </span>
              <div className="flex flex-col">
                <span className="font-medium">{budget.categoryName}</span>
                <span className="text-xs text-muted-foreground">
                  {formatCurrency(budget.spentAmount)}
                  {budget.limitAmount !== null ? ` de ${formatCurrency(budget.limitAmount)}` : ' gastos'}
                </span>
              </div>
            </div>

            <BudgetProgressBar limitAmount={budget.limitAmount} spentAmount={budget.spentAmount} />

            <div className="flex justify-end gap-2">
              <Button variant="outline" size="sm" onClick={() => onEdit(budget)}>
                {budget.limitAmount === null ? 'Definir limite' : 'Editar'}
              </Button>
              <Button variant="ghost" size="sm" onClick={() => onDelete(budget)}>
                Excluir
              </Button>
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  )
}
