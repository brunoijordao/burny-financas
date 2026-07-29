import { useCallback, useEffect, useState } from 'react'

import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import type { Account } from '@/features/accounts/api/accountsApi'
import type { Category } from '@/features/categories/api/categoriesApi'
import * as reportsApi from '@/features/reports/api/reportsApi'
import type { StatementLine } from '@/features/reports/api/reportsApi'
import { downloadBlob } from '@/features/reports/lib/download'
import { TransactionFiltersBar, type TransactionFiltersValue } from '@/features/transactions/components/TransactionFiltersBar'

function formatCurrency(value: number) {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

interface StatementReportViewProps {
  accounts: Account[]
  categories: Category[]
  filters: TransactionFiltersValue
  onFiltersChange: (value: TransactionFiltersValue) => void
}

/** Reuses TransactionFiltersBar so the statement report's filters stay identical to the Transactions page's own filters. */
export function StatementReportView({ accounts, categories, filters, onFiltersChange }: StatementReportViewProps) {
  const [lines, setLines] = useState<StatementLine[]>([])
  const [loadError, setLoadError] = useState<string | null>(null)
  const [exporting, setExporting] = useState(false)

  const hasPeriod = Boolean(filters.startDate && filters.endDate)

  const toApiFilters = useCallback(
    (): reportsApi.StatementFilters => ({
      startDate: filters.startDate,
      endDate: filters.endDate,
      accountId: filters.accountId ? Number(filters.accountId) : undefined,
      categoryId: filters.categoryId ? Number(filters.categoryId) : undefined,
      type: filters.type || undefined,
    }),
    [filters],
  )

  useEffect(() => {
    if (!hasPeriod) {
      setLines([])
      return
    }
    reportsApi
      .fetchStatement(toApiFilters())
      .then((result) => {
        setLines(result)
        setLoadError(null)
      })
      .catch(() => setLoadError('Não foi possível carregar o extrato. Tente novamente em instantes.'))
  }, [hasPeriod, toApiFilters])

  const handleExport = async (format: reportsApi.ReportExportFormat) => {
    setExporting(true)
    try {
      const blob = await reportsApi.exportStatement(toApiFilters(), format)
      downloadBlob(blob, `extrato_${filters.startDate}_${filters.endDate}.${format}`)
    } catch {
      setLoadError('Não foi possível gerar o arquivo. Tente novamente em instantes.')
    } finally {
      setExporting(false)
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <TransactionFiltersBar accounts={accounts} categories={categories} value={filters} onChange={onFiltersChange} />

      {loadError ? <p className="text-sm text-destructive">{loadError}</p> : null}

      {!hasPeriod ? (
        <p className="text-sm text-muted-foreground">Selecione o período (De/Até) para ver o extrato.</p>
      ) : lines.length === 0 ? (
        <p className="text-sm text-muted-foreground">Nenhuma transação encontrada para os filtros selecionados.</p>
      ) : (
        <div className="flex flex-col gap-2">
          {lines.map((line, index) => (
            <Card key={index}>
              <CardContent className="flex flex-wrap items-center justify-between gap-3 py-3">
                <div className="flex flex-col gap-0.5">
                  <span className="font-medium">{line.description}</span>
                  <span className="text-xs text-muted-foreground">
                    {line.transactionDate} · {line.accountName} · {line.categoryName}
                  </span>
                </div>
                <span
                  className={
                    line.type === 'EXPENSE' ? 'font-semibold text-destructive' : 'font-semibold text-emerald-600 dark:text-emerald-400'
                  }
                >
                  {line.type === 'EXPENSE' ? '-' : '+'}
                  {formatCurrency(line.amount)}
                </span>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <div className="flex gap-2">
        <Button type="button" variant="outline" disabled={!hasPeriod || exporting} onClick={() => void handleExport('pdf')}>
          Baixar PDF
        </Button>
        <Button type="button" variant="outline" disabled={!hasPeriod || exporting} onClick={() => void handleExport('xlsx')}>
          Baixar Excel
        </Button>
      </div>
    </div>
  )
}
