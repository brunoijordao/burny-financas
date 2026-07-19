import { z } from 'zod'

function isPositiveNumberString(value: string) {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0
}

export const setBudgetSchema = z.object({
  categoryId: z.string().min(1, 'Selecione uma categoria'),
  limitAmount: z.string().refine(isPositiveNumberString, 'Informe um valor maior que zero'),
})

export type SetBudgetFormValues = z.infer<typeof setBudgetSchema>
