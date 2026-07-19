import { z } from 'zod'

import { transactionTypes } from '@/features/transactions/schemas'

function isPositiveNumberString(value: string) {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0
}

export const editPdfImportItemSchema = z.object({
  transactionDate: z.string().min(1, 'Data é obrigatória'),
  description: z.string().min(1, 'Descrição é obrigatória'),
  amount: z.string().refine(isPositiveNumberString, 'Informe um valor maior que zero'),
  type: z.enum(transactionTypes),
  categoryId: z.string().optional(),
})

export type EditPdfImportItemFormValues = z.infer<typeof editPdfImportItemSchema>
