import { cn } from '@/lib/utils'
import type { PortfolioSummary } from '@/features/investments/api/investmentsApi'

function formatCurrency(value: number) {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

interface PortfolioSummaryCardProps {
  summary: PortfolioSummary
}

export function PortfolioSummaryCard({ summary }: PortfolioSummaryCardProps) {
  return (
    <div className="grid gap-4 sm:grid-cols-3">
      <div className="flex flex-col gap-1">
        <span className="text-xs text-muted-foreground">Valor investido</span>
        <span className="text-2xl font-semibold">{formatCurrency(summary.totalInvested)}</span>
      </div>
      <div className="flex flex-col gap-1">
        <span className="text-xs text-muted-foreground">Valor atual</span>
        <span className="text-2xl font-semibold">{formatCurrency(summary.totalCurrentValue)}</span>
      </div>
      <div className="flex flex-col gap-1">
        <span className="text-xs text-muted-foreground">Rentabilidade</span>
        <span
          className={cn(
            'text-2xl font-semibold',
            summary.profitabilityAmount >= 0 ? 'text-emerald-600 dark:text-emerald-400' : 'text-destructive',
          )}
        >
          {summary.profitabilityAmount >= 0 ? '+' : ''}
          {formatCurrency(summary.profitabilityAmount)}
          {summary.profitabilityPercentage !== null ? ` (${summary.profitabilityPercentage.toFixed(2)}%)` : ''}
        </span>
      </div>
    </div>
  )
}
