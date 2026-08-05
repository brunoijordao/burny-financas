import { formatCurrency } from '@/lib/formatters'
import { usePreferencesStore } from '@/features/settings/store/preferencesStore'

interface MonthSummaryRowProps {
  monthIncome: number
  monthExpense: number
  projectedBalance: number
}

/** Current-month income/expense detail plus the forward-looking projection (design.md Decision 3). */
export function MonthSummaryRow({ monthIncome, monthExpense, projectedBalance }: MonthSummaryRowProps) {
  const currency = usePreferencesStore((state) => state.currency)

  return (
    <div className="grid gap-3 sm:grid-cols-3">
      <div className="rounded-lg border border-border bg-card p-4">
        <p className="text-xs text-muted-foreground">Receitas do mês</p>
        <p className="text-xl font-semibold text-emerald-600 dark:text-emerald-400">{formatCurrency(monthIncome, currency)}</p>
      </div>
      <div className="rounded-lg border border-border bg-card p-4">
        <p className="text-xs text-muted-foreground">Despesas do mês</p>
        <p className="text-xl font-semibold text-destructive">{formatCurrency(monthExpense, currency)}</p>
      </div>
      <div className="rounded-lg border border-border bg-card p-4">
        <p className="text-xs text-muted-foreground">Saldo projetado no fim do mês</p>
        <p className="text-xl font-semibold">{formatCurrency(projectedBalance, currency)}</p>
      </div>
    </div>
  )
}
