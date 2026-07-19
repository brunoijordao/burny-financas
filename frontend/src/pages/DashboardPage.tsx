import { useCallback, useEffect, useState } from 'react'

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import * as accountsApi from '@/features/accounts/api/accountsApi'
import type { Account } from '@/features/accounts/api/accountsApi'
import * as dashboardApi from '@/features/dashboard/api/dashboardApi'
import type { DashboardSummary } from '@/features/dashboard/api/dashboardApi'
import { AccountCardRow } from '@/features/dashboard/components/AccountCardRow'
import { BalanceHero } from '@/features/dashboard/components/BalanceHero'
import { CategoryBreakdownChart } from '@/features/dashboard/components/CategoryBreakdownChart'
import { MonthlyTrendChart } from '@/features/dashboard/components/MonthlyTrendChart'
import { MonthSummaryRow } from '@/features/dashboard/components/MonthSummaryRow'
import { RecentTransactionsList } from '@/features/dashboard/components/RecentTransactionsList'
import { computeAvailableBalance, computeProjectedBalance, computeTotalInvoice } from '@/features/dashboard/lib/balances'
import * as transactionsApi from '@/features/transactions/api/transactionsApi'
import type { Transaction } from '@/features/transactions/api/transactionsApi'

const RECENT_TRANSACTIONS_SIZE = 10

export function DashboardPage() {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [recentTransactions, setRecentTransactions] = useState<Transaction[]>([])
  const [loadError, setLoadError] = useState<string | null>(null)

  const reload = useCallback(async () => {
    try {
      const [accountList, dashboardSummary, transactionsPage] = await Promise.all([
        accountsApi.fetchAccounts(),
        dashboardApi.fetchDashboardSummary(),
        transactionsApi.fetchTransactions({ size: RECENT_TRANSACTIONS_SIZE }),
      ])
      setAccounts(accountList)
      setSummary(dashboardSummary)
      setRecentTransactions(transactionsPage.content)
      setLoadError(null)
    } catch {
      setLoadError('Não foi possível carregar o dashboard. Tente novamente em instantes.')
    }
  }, [])

  useEffect(() => {
    void reload()
  }, [reload])

  if (loadError) {
    return (
      <div className="mx-auto max-w-5xl p-4 py-8">
        <p className="text-sm text-destructive">{loadError}</p>
      </div>
    )
  }

  if (!summary) {
    return (
      <div className="mx-auto max-w-5xl p-4 py-8">
        <p className="text-sm text-muted-foreground">Carregando...</p>
      </div>
    )
  }

  const availableBalance = computeAvailableBalance(accounts)
  const totalInvoice = computeTotalInvoice(accounts)
  const projectedBalance = computeProjectedBalance(availableBalance, summary.futureIncome, summary.futureExpense)

  return (
    <div className="mx-auto flex max-w-5xl flex-col gap-6 p-4 py-8">
      <BalanceHero availableBalance={availableBalance} totalInvoice={totalInvoice} monthNet={summary.monthNet} />

      <AccountCardRow accounts={accounts} />

      <MonthSummaryRow
        monthIncome={summary.monthIncome}
        monthExpense={summary.monthExpense}
        projectedBalance={projectedBalance}
      />

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Gastos por categoria</CardTitle>
          </CardHeader>
          <CardContent>
            <CategoryBreakdownChart items={summary.categoryBreakdown} />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Receitas x despesas (6 meses)</CardTitle>
          </CardHeader>
          <CardContent>
            <MonthlyTrendChart items={summary.monthlyTrend} />
          </CardContent>
        </Card>
      </div>

      <div>
        <h2 className="mb-3 text-lg font-semibold">Últimas transações</h2>
        <RecentTransactionsList transactions={recentTransactions} />
      </div>
    </div>
  )
}
