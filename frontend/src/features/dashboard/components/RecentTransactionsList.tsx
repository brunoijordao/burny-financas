import { ArrowDownRight, ArrowUpRight } from 'lucide-react'
import { Link } from 'react-router-dom'

import { cn } from '@/lib/utils'
import type { Transaction } from '@/features/transactions/api/transactionsApi'

function formatCurrency(value: number) {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

interface RecentTransactionsListProps {
  transactions: Transaction[]
}

/** Up to 10 most recent transactions, linking out to the full Transactions page (design.md Decision 5). */
export function RecentTransactionsList({ transactions }: RecentTransactionsListProps) {
  return (
    <div className="flex flex-col gap-3">
      {transactions.length === 0 ? (
        <p className="text-sm text-muted-foreground">Nenhuma transação registrada ainda.</p>
      ) : (
        <div className="flex flex-col divide-y divide-border rounded-lg border border-border bg-card">
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
                        : 'bg-emerald-600/10 text-emerald-600 dark:text-emerald-400',
                    )}
                  >
                    {isExpense ? <ArrowDownRight className="size-4" /> : <ArrowUpRight className="size-4" />}
                  </span>
                  <div className="flex flex-col">
                    <span className="text-sm font-medium">{transaction.description}</span>
                    <span className="text-xs text-muted-foreground">{transaction.transactionDate}</span>
                  </div>
                </div>
                <span
                  className={cn(
                    'font-semibold',
                    isExpense ? 'text-destructive' : 'text-emerald-600 dark:text-emerald-400',
                  )}
                >
                  {isExpense ? '-' : '+'}
                  {formatCurrency(transaction.amount)}
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
