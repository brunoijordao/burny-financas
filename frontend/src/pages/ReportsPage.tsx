import { useCallback, useEffect, useState } from 'react'

import { Button } from '@/components/ui/button'
import * as accountsApi from '@/features/accounts/api/accountsApi'
import type { Account } from '@/features/accounts/api/accountsApi'
import * as categoriesApi from '@/features/categories/api/categoriesApi'
import type { Category } from '@/features/categories/api/categoriesApi'
import type { CategorySpendingFilters } from '@/features/reports/api/reportsApi'
import { CategorySpendingReportView } from '@/features/reports/components/CategorySpendingReportView'
import { NetWorthEvolutionReportView } from '@/features/reports/components/NetWorthEvolutionReportView'
import { StatementReportView } from '@/features/reports/components/StatementReportView'
import type { TransactionFiltersValue } from '@/features/transactions/components/TransactionFiltersBar'

type ReportType = 'statement' | 'spending-by-category' | 'net-worth-evolution'

const REPORT_TYPE_LABELS: Record<ReportType, string> = {
  statement: 'Extrato por Período',
  'spending-by-category': 'Gastos por Categoria',
  'net-worth-evolution': 'Evolução do Patrimônio',
}

function currentMonthRange(): { startDate: string; endDate: string } {
  const now = new Date()
  const start = new Date(now.getFullYear(), now.getMonth(), 1)
  return { startDate: start.toISOString().slice(0, 10), endDate: now.toISOString().slice(0, 10) }
}

export function ReportsPage() {
  const [reportType, setReportType] = useState<ReportType>('statement')
  const [accounts, setAccounts] = useState<Account[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [statementFilters, setStatementFilters] = useState<TransactionFiltersValue>({
    accountId: '',
    categoryId: '',
    type: '',
    ...currentMonthRange(),
  })
  const [spendingPeriod, setSpendingPeriod] = useState<CategorySpendingFilters>(currentMonthRange())

  const reloadLookups = useCallback(async () => {
    const [accountList, categoryList] = await Promise.all([accountsApi.fetchAccounts(), categoriesApi.fetchCategories()])
    setAccounts(accountList)
    setCategories(categoryList)
  }, [])

  useEffect(() => {
    void reloadLookups()
  }, [reloadLookups])

  return (
    <div className="mx-auto flex max-w-5xl flex-col gap-6 p-4 py-8">
      <div>
        <h1 className="text-2xl font-semibold">Relatórios</h1>
        <p className="text-muted-foreground">Extrato, gastos por categoria e evolução do patrimônio, prontos para exportar.</p>
      </div>

      <div className="flex flex-wrap gap-2">
        {(Object.keys(REPORT_TYPE_LABELS) as ReportType[]).map((type) => (
          <Button key={type} type="button" variant={reportType === type ? 'default' : 'outline'} onClick={() => setReportType(type)}>
            {REPORT_TYPE_LABELS[type]}
          </Button>
        ))}
      </div>

      {reportType === 'statement' ? (
        <StatementReportView
          accounts={accounts}
          categories={categories}
          filters={statementFilters}
          onFiltersChange={setStatementFilters}
        />
      ) : null}

      {reportType === 'spending-by-category' ? (
        <CategorySpendingReportView period={spendingPeriod} onPeriodChange={setSpendingPeriod} />
      ) : null}

      {reportType === 'net-worth-evolution' ? <NetWorthEvolutionReportView /> : null}
    </div>
  )
}
