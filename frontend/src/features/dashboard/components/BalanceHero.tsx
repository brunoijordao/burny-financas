import { cn } from '@/lib/utils'
import { formatCurrency } from '@/lib/formatters'
import { usePreferencesStore } from '@/features/settings/store/preferencesStore'

interface BalanceHeroProps {
  availableBalance: number
  totalInvoice: number
  monthNet: number
}

/** The page's opening figure: available balance, with invoices and the month's net result as secondary context — not a uniform KPI grid (design.md Decision 4). */
export function BalanceHero({ availableBalance, totalInvoice, monthNet }: BalanceHeroProps) {
  const currency = usePreferencesStore((state) => state.currency)

  return (
    <div className="flex flex-col gap-4 rounded-lg border border-border bg-card p-6 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <p className="text-sm text-muted-foreground">Saldo disponível</p>
        <p className="text-4xl font-semibold tracking-tight sm:text-5xl">{formatCurrency(availableBalance, currency)}</p>
      </div>
      <div className="flex gap-6">
        <div>
          <p className="text-xs text-muted-foreground">Faturas em aberto</p>
          <p className="text-lg font-medium">{formatCurrency(totalInvoice, currency)}</p>
        </div>
        <div>
          <p className="text-xs text-muted-foreground">Resultado do mês</p>
          <p
            className={cn(
              'text-lg font-medium',
              monthNet >= 0 ? 'text-emerald-600 dark:text-emerald-400' : 'text-destructive',
            )}
          >
            {monthNet >= 0 ? '+' : ''}
            {formatCurrency(monthNet, currency)}
          </p>
        </div>
      </div>
    </div>
  )
}
