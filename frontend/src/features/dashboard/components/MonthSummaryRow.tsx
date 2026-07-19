function formatCurrency(value: number) {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

interface MonthSummaryRowProps {
  monthIncome: number
  monthExpense: number
  projectedBalance: number
}

/** Current-month income/expense detail plus the forward-looking projection (design.md Decision 3). */
export function MonthSummaryRow({ monthIncome, monthExpense, projectedBalance }: MonthSummaryRowProps) {
  return (
    <div className="grid gap-3 sm:grid-cols-3">
      <div className="rounded-lg border border-border bg-card p-4">
        <p className="text-xs text-muted-foreground">Receitas do mês</p>
        <p className="text-xl font-semibold text-emerald-600 dark:text-emerald-400">{formatCurrency(monthIncome)}</p>
      </div>
      <div className="rounded-lg border border-border bg-card p-4">
        <p className="text-xs text-muted-foreground">Despesas do mês</p>
        <p className="text-xl font-semibold text-destructive">{formatCurrency(monthExpense)}</p>
      </div>
      <div className="rounded-lg border border-border bg-card p-4">
        <p className="text-xs text-muted-foreground">Saldo projetado no fim do mês</p>
        <p className="text-xl font-semibold">{formatCurrency(projectedBalance)}</p>
      </div>
    </div>
  )
}
