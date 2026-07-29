import { httpClient } from '@/lib/httpClient'
import type { TransactionType } from '@/features/transactions/api/transactionsApi'

export type ReportExportFormat = 'pdf' | 'xlsx'

export interface StatementLine {
  transactionDate: string
  accountName: string
  categoryName: string
  type: TransactionType
  amount: number
  description: string
}

export interface StatementFilters {
  startDate: string
  endDate: string
  accountId?: number
  categoryId?: number
  type?: TransactionType
}

export interface CategorySpending {
  categoryId: number | null
  categoryName: string
  icon: string | null
  color: string | null
  total: number
  percentage: number
}

export interface CategorySpendingFilters {
  startDate: string
  endDate: string
}

export interface NetWorthEvolutionPoint {
  date: string
  totalValue: number
}

export interface NetWorthEvolutionReport {
  currentConsolidatedAccountBalance: number
  investmentNetWorthEvolution: NetWorthEvolutionPoint[]
}

/** GET /reports/statement -> the caller's transactions for a period, filterable by account/category/type */
export async function fetchStatement(filters: StatementFilters): Promise<StatementLine[]> {
  const response = await httpClient.get<StatementLine[]>('/reports/statement', { params: filters })
  return response.data
}

/** GET /reports/spending-by-category -> expense totals and percentages grouped by category for a period */
export async function fetchSpendingByCategory(filters: CategorySpendingFilters): Promise<CategorySpending[]> {
  const response = await httpClient.get<CategorySpending[]>('/reports/spending-by-category', { params: filters })
  return response.data
}

/** GET /reports/net-worth-evolution -> current consolidated account balance plus the investment net worth series */
export async function fetchNetWorthEvolutionReport(): Promise<NetWorthEvolutionReport> {
  const response = await httpClient.get<NetWorthEvolutionReport>('/reports/net-worth-evolution')
  return response.data
}

/** GET /reports/statement/export?format=pdf|xlsx -> downloads the server-generated report file as a Blob */
export async function exportStatement(filters: StatementFilters, format: ReportExportFormat): Promise<Blob> {
  const response = await httpClient.get('/reports/statement/export', {
    params: { ...filters, format },
    responseType: 'blob',
  })
  return response.data
}

/** GET /reports/spending-by-category/export?format=pdf|xlsx -> downloads the server-generated report file as a Blob */
export async function exportSpendingByCategory(
  filters: CategorySpendingFilters,
  format: ReportExportFormat,
): Promise<Blob> {
  const response = await httpClient.get('/reports/spending-by-category/export', {
    params: { ...filters, format },
    responseType: 'blob',
  })
  return response.data
}
