import { cn } from '@/lib/utils'
import { formatCurrency } from '@/lib/formatters'
import type { PortfolioSummary } from '@/features/investments/api/investmentsApi'
import { usePreferencesStore } from '@/features/settings/store/preferencesStore'

interface PortfolioSummaryCardProps {
  summary: PortfolioSummary
}

export function PortfolioSummaryCard({ summary }: PortfolioSummaryCardProps) {
  const currency = usePreferencesStore((state) => state.currency)

  return (
    <div className="grid gap-4 sm:grid-cols-3">
      <div className="flex flex-col gap-1">
        <span className="text-xs text-muted-foreground">Valor investido</span>
        <span className="text-2xl font-semibold">{formatCurrency(summary.totalInvested, currency)}</span>
      </div>
      <div className="flex flex-col gap-1">
        <span className="text-xs text-muted-foreground">Valor atual</span>
        <span className="text-2xl font-semibold">{formatCurrency(summary.totalCurrentValue, currency)}</span>
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
          {formatCurrency(summary.profitabilityAmount, currency)}
          {summary.profitabilityPercentage !== null ? ` (${summary.profitabilityPercentage.toFixed(2)}%)` : ''}
        </span>
      </div>
    </div>
  )
}
