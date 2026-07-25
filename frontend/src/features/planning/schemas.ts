import { z } from 'zod'

function isPositiveNumberString(value: string) {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0
}

export const planningEntrySchema = z.object({
  type: z.enum(['PAYABLE', 'RECEIVABLE']),
  accountId: z.string().min(1, 'Selecione uma conta'),
  categoryId: z.string().optional(),
  amount: z.string().refine(isPositiveNumberString, 'Informe um valor maior que zero'),
  description: z.string().min(1, 'Descrição é obrigatória'),
  dueDate: z.string().min(1, 'Data de vencimento é obrigatória'),
})

export type PlanningEntryFormValues = z.infer<typeof planningEntrySchema>
