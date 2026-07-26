import { z } from 'zod'

function isPositiveNumberString(value: string) {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0
}

export const assetSchema = z.object({
  name: z.string().min(1, 'Nome é obrigatório'),
  ticker: z.string().optional(),
  type: z.enum(['STOCK', 'FII', 'CDB', 'TREASURY_DIRECT', 'CRYPTO']),
  accountId: z.string().optional(),
})

export type AssetFormValues = z.infer<typeof assetSchema>

export const operationSchema = z.object({
  type: z.enum(['BUY', 'SELL']),
  quantity: z.string().refine(isPositiveNumberString, 'Informe uma quantidade maior que zero'),
  unitPrice: z.string().refine(isPositiveNumberString, 'Informe um preço maior que zero'),
  operationDate: z.string().min(1, 'Data é obrigatória'),
})

export type OperationFormValues = z.infer<typeof operationSchema>

export const valuationSchema = z.object({
  valueDate: z.string().min(1, 'Data é obrigatória'),
  totalValue: z.string().refine(isPositiveNumberString, 'Informe um valor maior que zero'),
})

export type ValuationFormValues = z.infer<typeof valuationSchema>

export const benchmarkComparisonSchema = z.object({
  benchmarkType: z.enum(['CDI', 'IBOVESPA', 'IPCA']),
  benchmarkPercentage: z.string().refine((value) => Number.isFinite(Number(value)), 'Informe um percentual válido'),
  periodStart: z.string().min(1, 'Data inicial é obrigatória'),
  periodEnd: z.string().min(1, 'Data final é obrigatória'),
})

export type BenchmarkComparisonFormValues = z.infer<typeof benchmarkComparisonSchema>
