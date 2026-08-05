import { Bar, BarChart, Cell, LabelList, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'

import { formatCurrency } from '@/lib/formatters'
import type { CategoryBreakdownItem } from '@/features/dashboard/api/dashboardApi'
import { usePreferencesStore } from '@/features/settings/store/preferencesStore'

const CATEGORY_COLORS = [
  'var(--chart-1)',
  'var(--chart-2)',
  'var(--chart-3)',
  'var(--chart-4)',
  'var(--chart-5)',
  'var(--chart-6)',
]
const OTHERS_COLOR = 'var(--muted-foreground)'
const MAX_SLICES = 6

interface ChartRow {
  name: string
  total: number
  color: string
}

function toChartRows(items: CategoryBreakdownItem[]): ChartRow[] {
  const top = items.slice(0, MAX_SLICES).map((item, index) => ({
    name: item.categoryName,
    total: item.total,
    color: CATEGORY_COLORS[index],
  }))
  const rest = items.slice(MAX_SLICES)
  if (rest.length > 0) {
    top.push({ name: 'Outros', total: rest.reduce((sum, item) => sum + item.total, 0), color: OTHERS_COLOR })
  }
  return top
}

interface CategoryBreakdownChartProps {
  items: CategoryBreakdownItem[]
}

/** Horizontal bar list of the month's top expense categories — proportions read faster as ranked bars than pie wedges (design.md Decision 4). */
export function CategoryBreakdownChart({ items }: CategoryBreakdownChartProps) {
  const currency = usePreferencesStore((state) => state.currency)

  if (items.length === 0) {
    return <p className="text-sm text-muted-foreground">Nenhuma despesa registrada neste mês.</p>
  }

  const rows = toChartRows(items)

  return (
    <ResponsiveContainer width="100%" height={Math.max(rows.length * 44, 120)}>
      <BarChart data={rows} layout="vertical" margin={{ top: 4, right: 48, bottom: 4, left: 4 }}>
        <XAxis type="number" hide />
        <YAxis type="category" dataKey="name" width={110} tickLine={false} axisLine={false} fontSize={12} />
        <Tooltip formatter={(value) => formatCurrency(Number(value ?? 0), currency)} cursor={{ fill: 'var(--muted)' }} />
        <Bar dataKey="total" radius={4} maxBarSize={24}>
          {rows.map((row) => (
            <Cell key={row.name} fill={row.color} />
          ))}
          <LabelList dataKey="total" position="right" formatter={(value) => formatCurrency(Number(value ?? 0), currency)} fontSize={12} />
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  )
}
