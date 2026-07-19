import { z } from 'zod'

function isPositiveNumberString(value: string) {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0
}

function isFutureDateString(value: string) {
  const date = new Date(value + 'T00:00:00')
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() > today.getTime()
}

export const goalSchema = z.object({
  name: z.string().min(1, 'Nome é obrigatório'),
  targetAmount: z.string().refine(isPositiveNumberString, 'Informe um valor maior que zero'),
  deadline: z.string().refine(isFutureDateString, 'O prazo deve ser uma data futura'),
})

export type GoalFormValues = z.infer<typeof goalSchema>

export const contributionSchema = z.object({
  amount: z.string().refine(isPositiveNumberString, 'Informe um valor maior que zero'),
  contributionDate: z.string().optional(),
})

export type ContributionFormValues = z.infer<typeof contributionSchema>
