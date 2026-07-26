import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'

import type { NetWorthEvolutionPoint } from '@/features/investments/api/investmentsApi'

function formatDate(value: string) {
  return new Date(value + 'T00:00:00').toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' })
}

function formatCurrency(value: number) {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

interface NetWorthEvolutionChartProps {
  points: NetWorthEvolutionPoint[]
}

/** Follows the same axis/tooltip/grid conventions as the dashboard's MonthlyTrendChart. Points only exist where the user has recorded at least one valuation — there is no quote feed to fill the gaps (design.md Decision 2). */
export function NetWorthEvolutionChart({ points }: NetWorthEvolutionChartProps) {
  if (points.length === 0) {
    return <p className="text-sm text-muted-foreground">Registre o valor atual de um ativo para começar a ver a evolução do patrimônio.</p>
  }

  const rows = points.map((point) => ({ ...point, label: formatDate(point.date) }))

  return (
    <ResponsiveContainer width="100%" height={260}>
      <LineChart data={rows} margin={{ top: 4, right: 8, bottom: 4, left: 8 }}>
        <CartesianGrid vertical={false} stroke="var(--border)" />
        <XAxis dataKey="label" tickLine={false} axisLine={false} fontSize={12} />
        <YAxis hide />
        <Tooltip formatter={(value) => formatCurrency(Number(value ?? 0))} cursor={{ stroke: 'var(--muted)' }} />
        <Line type="monotone" dataKey="totalValue" name="Patrimônio" stroke="var(--primary)" strokeWidth={2} dot={{ r: 3 }} />
      </LineChart>
    </ResponsiveContainer>
  )
}
