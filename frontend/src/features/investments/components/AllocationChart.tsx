import { Bar, BarChart, Cell, LabelList, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'

import type { AllocationItem, AssetType } from '@/features/investments/api/investmentsApi'

const TYPE_LABELS: Record<AssetType, string> = {
  STOCK: 'Ações',
  FII: 'FIIs',
  CDB: 'CDB',
  TREASURY_DIRECT: 'Tesouro Direto',
  CRYPTO: 'Criptomoedas',
}

const TYPE_COLORS = ['var(--chart-1)', 'var(--chart-2)', 'var(--chart-3)', 'var(--chart-4)', 'var(--chart-5)']

interface AllocationChartProps {
  items: AllocationItem[]
}

/** Horizontal ranked bars, same form as the dashboard's CategoryBreakdownChart — consistent reading pattern for proportions across the app. */
export function AllocationChart({ items }: AllocationChartProps) {
  if (items.length === 0) {
    return <p className="text-sm text-muted-foreground">Nenhum ativo cadastrado ainda.</p>
  }

  const rows = items
    .slice()
    .sort((a, b) => b.percentage - a.percentage)
    .map((item, index) => ({
      name: TYPE_LABELS[item.type],
      percentage: item.percentage,
      color: TYPE_COLORS[index % TYPE_COLORS.length],
    }))

  return (
    <ResponsiveContainer width="100%" height={Math.max(rows.length * 44, 120)}>
      <BarChart data={rows} layout="vertical" margin={{ top: 4, right: 48, bottom: 4, left: 4 }}>
        <XAxis type="number" hide domain={[0, 100]} />
        <YAxis type="category" dataKey="name" width={110} tickLine={false} axisLine={false} fontSize={12} />
        <Tooltip formatter={(value) => `${Number(value ?? 0).toFixed(2)}%`} cursor={{ fill: 'var(--muted)' }} />
        <Bar dataKey="percentage" radius={4} maxBarSize={24}>
          {rows.map((row) => (
            <Cell key={row.name} fill={row.color} />
          ))}
          <LabelList dataKey="percentage" position="right" formatter={(value) => `${Number(value ?? 0).toFixed(1)}%`} fontSize={12} />
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  )
}
