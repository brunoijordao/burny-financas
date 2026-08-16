import { ArrowDownRight, ArrowUpRight } from 'lucide-react'
import { Link } from 'react-router-dom'

import { cn } from '@/lib/utils'
import { formatCurrency, formatDate } from '@/lib/formatters'
import type { Transaction } from '@/features/transactions/api/transactionsApi'
import { usePreferencesStore } from '@/features/settings/store/preferencesStore'

interface RecentTransactionsListProps {
  transactions: Transaction[]
}

/** Up to 10 most recent transactions, linking out to the full Transactions page (design.md Decision 5). */
export function RecentTransactionsList({ transactions }: RecentTransactionsListProps) {
  const currency = usePreferencesStore((state) => state.currency)
  const dateFormat = usePreferencesStore((state) => state.dateFormat)

  return (
    <div className="flex flex-col gap-3">
      {transactions.length === 0 ? (
        <p className="text-sm text-muted-foreground">Nenhuma transação registrada ainda.</p>
      ) : (
        <div className="flex flex-col divide-y divide-border rounded-card border border-border bg-card">
          {transactions.map((transaction) => {
            const isExpense = transaction.type === 'EXPENSE'
            return (
              <div key={transaction.id} className="flex items-center justify-between gap-3 px-4 py-3">
                <div className="flex items-center gap-3">
                  <span
                    className={cn(
                      'flex size-8 shrink-0 items-center justify-center rounded-full',
                      isExpense
                        ? 'bg-destructive/10 text-destructive'
                        : 'bg-secondary text-foreground',
                    )}
                  >
                    {isExpense ? <ArrowDownRight className="size-4" /> : <ArrowUpRight className="size-4" />}
                  </span>
                  <div className="flex flex-col">
                    <span className="text-sm font-medium">{transaction.description}</span>
                    <span className="text-xs text-muted-foreground">{formatDate(transaction.transactionDate, dateFormat)}</span>
                  </div>
                </div>
                <span
                  className={cn(
                    'font-semibold',
                    isExpense ? 'text-destructive' : 'text-foreground',
                  )}
                >
                  {isExpense ? '-' : '+'}
                  {formatCurrency(transaction.amount, currency)}
                </span>
              </div>
            )
          })}
        </div>
      )}

      <Link to="/transactions" className="text-sm font-medium text-primary hover:underline">
        Ver todas as transações
      </Link>
    </div>
  )
}
