import { z } from 'zod'

export const transactionTypes = ['EXPENSE', 'INCOME'] as const
export type TransactionTypeValue = (typeof transactionTypes)[number]

export const transactionTypeLabels: Record<TransactionTypeValue, string> = {
  EXPENSE: 'Despesa',
  INCOME: 'Receita',
}

export const recurrenceFrequencies = ['WEEKLY', 'MONTHLY', 'YEARLY'] as const
export type RecurrenceFrequencyValue = (typeof recurrenceFrequencies)[number]

export const recurrenceFrequencyLabels: Record<RecurrenceFrequencyValue, string> = {
  WEEKLY: 'Semanal',
  MONTHLY: 'Mensal',
  YEARLY: 'Anual',
}

function isPositiveNumberString(value: string) {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0
}

export const createTransactionSchema = z
  .object({
    description: z.string().min(1, 'Descrição é obrigatória'),
    amount: z.string().refine(isPositiveNumberString, 'Informe um valor maior que zero'),
    type: z.enum(transactionTypes),
    transactionDate: z.string().min(1, 'Data é obrigatória'),
    accountId: z.string().min(1, 'Selecione uma conta'),
    categoryId: z.string().optional(),
    note: z.string().optional(),
    recurring: z.boolean(),
    frequency: z.enum(recurrenceFrequencies).optional(),
    startDate: z.string().optional(),
    endDate: z.string().optional(),
  })
  .refine((data) => !data.recurring || !!data.frequency, {
    message: 'Selecione a frequência da recorrência',
    path: ['frequency'],
  })
  .refine((data) => !data.recurring || !!data.startDate, {
    message: 'Informe a data de início da recorrência',
    path: ['startDate'],
  })

export type CreateTransactionFormValues = z.infer<typeof createTransactionSchema>

export const editTransactionSchema = z.object({
  description: z.string().min(1, 'Descrição é obrigatória'),
  amount: z.string().refine(isPositiveNumberString, 'Informe um valor maior que zero'),
  type: z.enum(transactionTypes),
  transactionDate: z.string().min(1, 'Data é obrigatória'),
  accountId: z.string().min(1, 'Selecione uma conta'),
  categoryId: z.string().optional(),
  note: z.string().optional(),
})

export type EditTransactionFormValues = z.infer<typeof editTransactionSchema>

export const filtersSchema = z.object({
  accountId: z.string().optional(),
  categoryId: z.string().optional(),
  type: z.enum(transactionTypes).optional(),
  startDate: z.string().optional(),
  endDate: z.string().optional(),
})

export type FiltersFormValues = z.infer<typeof filtersSchema>
