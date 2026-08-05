import { useEffect, useState } from 'react'

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { formatCurrency } from '@/lib/formatters'
import { NetWorthEvolutionChart } from '@/features/investments/components/NetWorthEvolutionChart'
import * as reportsApi from '@/features/reports/api/reportsApi'
import type { NetWorthEvolutionReport } from '@/features/reports/api/reportsApi'
import { usePreferencesStore } from '@/features/settings/store/preferencesStore'

/** No PDF/Excel export here — this report stays preview/chart-only in this change (design.md Non-Goals). */
export function NetWorthEvolutionReportView() {
  const currency = usePreferencesStore((state) => state.currency)
  const [report, setReport] = useState<NetWorthEvolutionReport | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)

  useEffect(() => {
    reportsApi
      .fetchNetWorthEvolutionReport()
      .then((result) => {
        setReport(result)
        setLoadError(null)
      })
      .catch(() => setLoadError('Não foi possível carregar o relatório. Tente novamente em instantes.'))
  }, [])

  if (loadError) {
    return <p className="text-sm text-destructive">{loadError}</p>
  }

  if (!report) {
    return null
  }

  return (
    <div className="flex flex-col gap-4">
      <Card>
        <CardHeader>
          <CardTitle>Saldo consolidado atual</CardTitle>
          <CardDescription>Soma do saldo das suas contas ativas hoje.</CardDescription>
        </CardHeader>
        <CardContent>
          <span className="text-2xl font-semibold">{formatCurrency(report.currentConsolidatedAccountBalance, currency)}</span>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Evolução do patrimônio investido</CardTitle>
          <CardDescription>Mesma série de evolução patrimonial exibida na página de Investimentos.</CardDescription>
        </CardHeader>
        <CardContent>
          <NetWorthEvolutionChart points={report.investmentNetWorthEvolution} />
        </CardContent>
      </Card>
    </div>
  )
}
