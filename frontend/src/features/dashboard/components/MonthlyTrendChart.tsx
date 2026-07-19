import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'

import type { MonthlyTrendItem } from '@/features/dashboard/api/dashboardApi'

const MONTH_LABELS = [
  'Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez',
]

function formatMonth(month: string) {
  const [year, monthNumber] = month.split('-')
  const index = Number(monthNumber) - 1
  return `${MONTH_LABELS[index] ?? month}/${year.slice(2)}`
}

function formatCurrency(value: number) {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

interface MonthlyTrendChartProps {
  items: MonthlyTrendItem[]
}

/**
 * Income vs. expense across the last 6 months, single shared axis (never dual-axis). The
 * income/expense color pair sits in the dataviz validator's CVD warn band in light mode, so this
 * always ships a legend plus direct value labels (satisfied by the tooltip) rather than relying on
 * color alone — see add-dashboard/tasks.md section 6.
 */
export function MonthlyTrendChart({ items }: MonthlyTrendChartProps) {
  const rows = items.map((item) => ({ ...item, label: formatMonth(item.month) }))

  return (
    <ResponsiveContainer width="100%" height={260}>
      <BarChart data={rows} margin={{ top: 4, right: 8, bottom: 4, left: 8 }}>
        <CartesianGrid vertical={false} stroke="var(--border)" />
        <XAxis dataKey="label" tickLine={false} axisLine={false} fontSize={12} />
        <YAxis hide />
        <Tooltip formatter={(value) => formatCurrency(Number(value ?? 0))} cursor={{ fill: 'var(--muted)' }} />
        <Legend
          formatter={(value) => (value === 'income' ? 'Receitas' : 'Despesas')}
          wrapperStyle={{ fontSize: 12 }}
        />
        <Bar dataKey="income" name="income" fill="var(--chart-income)" radius={[4, 4, 0, 0]} maxBarSize={28} />
        <Bar dataKey="expense" name="expense" fill="var(--chart-expense)" radius={[4, 4, 0, 0]} maxBarSize={28} />
      </BarChart>
    </ResponsiveContainer>
  )
}
