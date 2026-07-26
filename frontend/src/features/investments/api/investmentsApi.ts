import { httpClient } from '@/lib/httpClient'

export type AssetType = 'STOCK' | 'FII' | 'CDB' | 'TREASURY_DIRECT' | 'CRYPTO'
export type OperationType = 'BUY' | 'SELL'
export type BenchmarkType = 'CDI' | 'IBOVESPA' | 'IPCA'

export interface InvestmentAsset {
  id: number
  name: string
  ticker: string | null
  type: AssetType
  accountId: number | null
  accountName: string | null
  quantity: number
  averagePrice: number
  investedAmount: number
  currentValue: number | null
  profitabilityAmount: number | null
  profitabilityPercentage: number | null
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateInvestmentAssetPayload {
  name: string
  ticker?: string
  type: AssetType
  accountId?: number
}

export type UpdateInvestmentAssetPayload = CreateInvestmentAssetPayload

export interface InvestmentOperation {
  id: number
  assetId: number
  type: OperationType
  quantity: number
  unitPrice: number
  operationDate: string
  createdAt: string
}

export interface CreateInvestmentOperationPayload {
  type: OperationType
  quantity: number
  unitPrice: number
  operationDate: string
}

export interface InvestmentValuation {
  id: number
  assetId: number
  valueDate: string
  totalValue: number
  createdAt: string
}

export interface CreateInvestmentValuationPayload {
  valueDate: string
  totalValue: number
}

export interface PortfolioSummary {
  totalInvested: number
  totalCurrentValue: number
  profitabilityAmount: number
  profitabilityPercentage: number | null
}

export interface AllocationItem {
  type: AssetType
  currentValue: number
  percentage: number
}

export interface NetWorthEvolutionPoint {
  date: string
  totalValue: number
}

export interface BenchmarkComparison {
  benchmarkType: BenchmarkType
  benchmarkPercentage: number
  portfolioReturnPercentage: number | null
  periodStart: string
  periodEnd: string
}

/** POST /investments/assets */
export async function createInvestmentAsset(payload: CreateInvestmentAssetPayload): Promise<InvestmentAsset> {
  const response = await httpClient.post<InvestmentAsset>('/investments/assets', payload)
  return response.data
}

/** GET /investments/assets -> the caller's active assets, with computed position and profitability */
export async function fetchInvestmentAssets(): Promise<InvestmentAsset[]> {
  const response = await httpClient.get<InvestmentAsset[]>('/investments/assets')
  return response.data
}

/** PUT /investments/assets/{id} */
export async function updateInvestmentAsset(id: number, payload: UpdateInvestmentAssetPayload): Promise<InvestmentAsset> {
  const response = await httpClient.put<InvestmentAsset>(`/investments/assets/${id}`, payload)
  return response.data
}

/** DELETE /investments/assets/{id}, soft-deleted server-side, preserving operations/valuations */
export async function deleteInvestmentAsset(id: number): Promise<void> {
  await httpClient.delete(`/investments/assets/${id}`)
}

/** POST /investments/assets/{assetId}/operations */
export async function createInvestmentOperation(
  assetId: number,
  payload: CreateInvestmentOperationPayload,
): Promise<InvestmentOperation> {
  const response = await httpClient.post<InvestmentOperation>(`/investments/assets/${assetId}/operations`, payload)
  return response.data
}

/** GET /investments/assets/{assetId}/operations */
export async function fetchInvestmentOperations(assetId: number): Promise<InvestmentOperation[]> {
  const response = await httpClient.get<InvestmentOperation[]>(`/investments/assets/${assetId}/operations`)
  return response.data
}

/** DELETE /investments/assets/{assetId}/operations/{id}, soft-deleted server-side */
export async function deleteInvestmentOperation(assetId: number, id: number): Promise<void> {
  await httpClient.delete(`/investments/assets/${assetId}/operations/${id}`)
}

/** POST /investments/assets/{assetId}/valuations */
export async function createInvestmentValuation(
  assetId: number,
  payload: CreateInvestmentValuationPayload,
): Promise<InvestmentValuation> {
  const response = await httpClient.post<InvestmentValuation>(`/investments/assets/${assetId}/valuations`, payload)
  return response.data
}

/** GET /investments/assets/{assetId}/valuations */
export async function fetchInvestmentValuations(assetId: number): Promise<InvestmentValuation[]> {
  const response = await httpClient.get<InvestmentValuation[]>(`/investments/assets/${assetId}/valuations`)
  return response.data
}

/** GET /investments/portfolio/summary */
export async function fetchPortfolioSummary(): Promise<PortfolioSummary> {
  const response = await httpClient.get<PortfolioSummary>('/investments/portfolio/summary')
  return response.data
}

/** GET /investments/portfolio/allocation */
export async function fetchPortfolioAllocation(): Promise<AllocationItem[]> {
  const response = await httpClient.get<AllocationItem[]>('/investments/portfolio/allocation')
  return response.data
}

/** GET /investments/portfolio/net-worth-evolution */
export async function fetchNetWorthEvolution(): Promise<NetWorthEvolutionPoint[]> {
  const response = await httpClient.get<NetWorthEvolutionPoint[]>('/investments/portfolio/net-worth-evolution')
  return response.data
}

/** GET /investments/portfolio/benchmark-comparison?benchmark=&percentage=&periodStart=&periodEnd= */
export async function fetchBenchmarkComparison(
  benchmark: BenchmarkType,
  percentage: number,
  periodStart: string,
  periodEnd: string,
): Promise<BenchmarkComparison> {
  const response = await httpClient.get<BenchmarkComparison>('/investments/portfolio/benchmark-comparison', {
    params: { benchmark, percentage, periodStart, periodEnd },
  })
  return response.data
}
