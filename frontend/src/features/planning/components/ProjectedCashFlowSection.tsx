import { useEffect, useState } from 'react'
import { Bar, CartesianGrid, ComposedChart, Legend, Line, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'

import { formatCurrency } from '@/lib/formatters'
import * as planningApi from '@/features/planning/api/planningApi'
import type { ProjectedCashFlow } from '@/features/planning/api/planningApi'
import { usePreferencesStore } from '@/features/settings/store/preferencesStore'

const PROJECTION_MONTHS = 6
const MONTH_LABELS = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez']

function formatMonth(month: string) {
  const [year, monthNumber] = month.split('-')
  const index = Number(monthNumber) - 1
  return `${MONTH_LABELS[index] ?? month}/${year.slice(2)}`
}

interface ProjectedCashFlowSectionProps {
  /** Bump this from the parent after a mutation (create/edit/settle/delete) to force a refetch. */
  refreshSignal?: number
}

/**
 * Fetches GET /planning-entries/projected-cash-flow on mount or when refreshSignal changes.
 * Distinct from the dashboard's single-month projection (which only looks at transactions already
 * recorded) — this one folds in every pending payable/receivable across several future months
 * (design.md Decision 4).
 */
export function ProjectedCashFlowSection({ refreshSignal }: ProjectedCashFlowSectionProps) {
  const currency = usePreferencesStore((state) => state.currency)
  const [data, setData] = useState<ProjectedCashFlow | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    planningApi
      .fetchProjectedCashFlow(PROJECTION_MONTHS)
      .then(setData)
      .catch(() => setError('Não foi possível carregar o fluxo de caixa projetado.'))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [refreshSignal])

  if (error) {
    return <p className="text-sm text-destructive">{error}</p>
  }

  if (!data) {
    return <p className="text-sm text-muted-foreground">Carregando...</p>
  }

  const rows = data.periods.map((period) => ({ ...period, label: formatMonth(period.month) }))

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <span className="text-xs text-muted-foreground">Saldo disponível atual</span>
        <span className="text-2xl font-semibold">{formatCurrency(data.currentAvailableBalance, currency)}</span>
      </div>

      <ResponsiveContainer width="100%" height={260}>
        <ComposedChart data={rows} margin={{ top: 4, right: 8, bottom: 4, left: 8 }}>
          <CartesianGrid vertical={false} stroke="var(--border)" />
          <XAxis dataKey="label" tickLine={false} axisLine={false} fontSize={12} />
          <YAxis hide />
          <Tooltip formatter={(value) => formatCurrency(Number(value ?? 0), currency)} cursor={{ fill: 'var(--muted)' }} />
          <Legend
            formatter={(value) => {
              if (value === 'totalReceivable') return 'A receber'
              if (value === 'totalPayable') return 'A pagar'
              return 'Saldo projetado'
            }}
            wrapperStyle={{ fontSize: 12 }}
          />
          <Bar dataKey="totalReceivable" name="totalReceivable" fill="var(--chart-income)" radius={[4, 4, 0, 0]} maxBarSize={28} />
          <Bar dataKey="totalPayable" name="totalPayable" fill="var(--chart-expense)" radius={[4, 4, 0, 0]} maxBarSize={28} />
          <Line
            type="monotone"
            dataKey="projectedBalance"
            name="projectedBalance"
            stroke="var(--primary)"
            strokeWidth={2}
            dot={{ r: 3 }}
          />
        </ComposedChart>
      </ResponsiveContainer>
    </div>
  )
}
