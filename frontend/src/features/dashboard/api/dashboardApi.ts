import { httpClient } from '@/lib/httpClient'

export interface CategoryBreakdownItem {
  categoryId: number | null
  categoryName: string
  icon: string | null
  color: string | null
  total: number
}

export interface MonthlyTrendItem {
  month: string
  income: number
  expense: number
}

export interface DashboardSummary {
  month: string
  monthIncome: number
  monthExpense: number
  monthNet: number
  futureIncome: number
  futureExpense: number
  categoryBreakdown: CategoryBreakdownItem[]
  monthlyTrend: MonthlyTrendItem[]
}

/** GET /dashboard/summary, scoped to the month containing `referenceDate` (defaults to today server-side). */
export async function fetchDashboardSummary(referenceDate?: string): Promise<DashboardSummary> {
  const response = await httpClient.get<DashboardSummary>('/dashboard/summary', {
    params: referenceDate ? { referenceDate } : undefined,
  })
  return response.data
}
