import { z } from 'zod'

export const accountTypes = [
  'CHECKING',
  'SAVINGS',
  'PHYSICAL_WALLET',
  'BROKERAGE',
  'DIGITAL_WALLET',
  'CREDIT_CARD',
] as const

export type AccountTypeValue = (typeof accountTypes)[number]

export const accountTypeLabels: Record<AccountTypeValue, string> = {
  CHECKING: 'Conta corrente',
  SAVINGS: 'Poupança',
  PHYSICAL_WALLET: 'Carteira física',
  BROKERAGE: 'Corretora',
  DIGITAL_WALLET: 'Conta digital',
  CREDIT_CARD: 'Cartão de crédito',
}

function isPositiveNumberString(value: string) {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0
}

export const createAccountSchema = z
  .object({
    name: z.string().min(1, 'Nome é obrigatório'),
    icon: z.string().min(1, 'Ícone é obrigatório'),
    color: z.string().min(1, 'Cor é obrigatória'),
    type: z.enum(accountTypes),
    creditLimit: z.string().optional(),
  })
  .refine(
    (data) => data.type !== 'CREDIT_CARD' || (!!data.creditLimit && isPositiveNumberString(data.creditLimit)),
    {
      message: 'Informe um limite maior que zero para cartão de crédito',
      path: ['creditLimit'],
    },
  )

export type CreateAccountFormValues = z.infer<typeof createAccountSchema>

export const editAccountSchema = z.object({
  name: z.string().min(1, 'Nome é obrigatório'),
  icon: z.string().min(1, 'Ícone é obrigatório'),
  color: z.string().min(1, 'Cor é obrigatória'),
  creditLimit: z.string().optional(),
})

export type EditAccountFormValues = z.infer<typeof editAccountSchema>

export const transferSchema = z
  .object({
    sourceAccountId: z.string().min(1, 'Selecione a conta de origem'),
    destinationAccountId: z.string().min(1, 'Selecione a conta de destino'),
    amount: z.string().refine(isPositiveNumberString, 'Informe um valor maior que zero'),
  })
  .refine((data) => data.sourceAccountId !== data.destinationAccountId, {
    message: 'Origem e destino devem ser contas diferentes',
    path: ['destinationAccountId'],
  })

export type TransferFormValues = z.infer<typeof transferSchema>
