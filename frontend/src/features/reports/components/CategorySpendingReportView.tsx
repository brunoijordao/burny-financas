import { useEffect, useState } from 'react'

import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { formatCurrency } from '@/lib/formatters'
import { CategoryBreakdownChart } from '@/features/dashboard/components/CategoryBreakdownChart'
import * as reportsApi from '@/features/reports/api/reportsApi'
import type { CategorySpending, CategorySpendingFilters } from '@/features/reports/api/reportsApi'
import { downloadBlob } from '@/features/reports/lib/download'
import { usePreferencesStore } from '@/features/settings/store/preferencesStore'

interface CategorySpendingReportViewProps {
  period: CategorySpendingFilters
  onPeriodChange: (period: CategorySpendingFilters) => void
}

export function CategorySpendingReportView({ period, onPeriodChange }: CategorySpendingReportViewProps) {
  const currency = usePreferencesStore((state) => state.currency)
  const [categories, setCategories] = useState<CategorySpending[]>([])
  const [loadError, setLoadError] = useState<string | null>(null)
  const [exporting, setExporting] = useState(false)

  const hasPeriod = Boolean(period.startDate && period.endDate)

  useEffect(() => {
    if (!hasPeriod) {
      setCategories([])
      return
    }
    reportsApi
      .fetchSpendingByCategory(period)
      .then((result) => {
        setCategories(result)
        setLoadError(null)
      })
      .catch(() => setLoadError('Não foi possível carregar o relatório. Tente novamente em instantes.'))
  }, [hasPeriod, period])

  const handleExport = async (format: reportsApi.ReportExportFormat) => {
    setExporting(true)
    try {
      const blob = await reportsApi.exportSpendingByCategory(period, format)
      downloadBlob(blob, `gastos-por-categoria_${period.startDate}_${period.endDate}.${format}`)
    } catch {
      setLoadError('Não foi possível gerar o arquivo. Tente novamente em instantes.')
    } finally {
      setExporting(false)
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="grid gap-3 sm:grid-cols-2">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="spending-start">De</Label>
          <Input
            id="spending-start"
            type="date"
            value={period.startDate}
            onChange={(event) => onPeriodChange({ ...period, startDate: event.target.value })}
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="spending-end">Até</Label>
          <Input
            id="spending-end"
            type="date"
            value={period.endDate}
            onChange={(event) => onPeriodChange({ ...period, endDate: event.target.value })}
          />
        </div>
      </div>

      {loadError ? <p className="text-sm text-destructive">{loadError}</p> : null}

      {!hasPeriod ? (
        <p className="text-sm text-muted-foreground">Selecione o período (De/Até) para ver os gastos por categoria.</p>
      ) : (
        <>
          <CategoryBreakdownChart items={categories} />

          <div className="flex flex-col gap-2">
            {categories.map((category) => (
              <Card key={category.categoryId ?? 'uncategorized'}>
                <CardContent className="flex items-center justify-between gap-3 py-3">
                  <span className="font-medium">{category.categoryName}</span>
                  <span className="text-sm text-muted-foreground">
                    {formatCurrency(category.total, currency)} · {category.percentage.toFixed(2)}%
                  </span>
                </CardContent>
              </Card>
            ))}
          </div>
        </>
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
