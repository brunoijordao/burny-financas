import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { isAxiosError } from 'axios'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import type { Account } from '@/features/accounts/api/accountsApi'
import type { Category } from '@/features/categories/api/categoriesApi'
import type { Transaction } from '@/features/transactions/api/transactionsApi'
import {
  editTransactionSchema,
  transactionTypeLabels,
  transactionTypes,
  type EditTransactionFormValues,
} from '@/features/transactions/schemas'

const selectClassName =
  'flex h-10 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring'

function flattenCategories(categories: Category[]): { id: number; label: string }[] {
  return categories.flatMap((category) => [
    { id: category.id, label: category.name },
    ...category.subcategories.map((sub) => ({ id: sub.id, label: `— ${sub.name}` })),
  ])
}

interface EditTransactionFormProps {
  transaction: Transaction
  accounts: Account[]
  categories: Category[]
  onSubmit: (values: EditTransactionFormValues) => Promise<void>
  onCancel: () => void
}

export function EditTransactionForm({ transaction, accounts, categories, onSubmit, onCancel }: EditTransactionFormProps) {
  const [formError, setFormError] = useState<string | null>(null)
  const flatCategories = flattenCategories(categories)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<EditTransactionFormValues>({
    resolver: zodResolver(editTransactionSchema),
    defaultValues: {
      description: transaction.description,
      amount: String(transaction.amount),
      type: transaction.type,
      transactionDate: transaction.transactionDate,
      accountId: String(transaction.accountId),
      categoryId: transaction.categoryId != null ? String(transaction.categoryId) : '',
      note: transaction.note ?? '',
    },
  })

  const submit = handleSubmit(async (values) => {
    setFormError(null)
    try {
      await onSubmit(values)
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 400) {
        setFormError('Dados inválidos. Verifique os campos e tente novamente.')
      } else if (isAxiosError(error) && error.response?.status === 404) {
        setFormError('Conta ou categoria não encontrada.')
      } else {
        setFormError('Não foi possível salvar as alterações. Tente novamente em instantes.')
      }
    }
  })

  return (
    <form className="flex flex-col gap-4" onSubmit={submit} noValidate>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="edit-description">Descrição</Label>
        <Input id="edit-description" aria-invalid={Boolean(errors.description)} {...register('description')} />
        {errors.description ? <p className="text-sm text-destructive">{errors.description.message}</p> : null}
      </div>

      <div className="flex gap-4">
        <div className="flex flex-1 flex-col gap-1.5">
          <Label htmlFor="edit-type">Tipo</Label>
          <select id="edit-type" className={selectClassName} {...register('type')}>
            {transactionTypes.map((type) => (
              <option key={type} value={type}>
                {transactionTypeLabels[type]}
              </option>
            ))}
          </select>
        </div>

        <div className="flex flex-1 flex-col gap-1.5">
          <Label htmlFor="edit-amount">Valor</Label>
          <Input
            id="edit-amount"
            type="number"
            step="0.01"
            aria-invalid={Boolean(errors.amount)}
            {...register('amount')}
          />
          {errors.amount ? <p className="text-sm text-destructive">{errors.amount.message}</p> : null}
        </div>
      </div>

      <div className="flex gap-4">
        <div className="flex flex-1 flex-col gap-1.5">
          <Label htmlFor="edit-accountId">Conta</Label>
          <select id="edit-accountId" className={selectClassName} {...register('accountId')}>
            {accounts.map((account) => (
              <option key={account.id} value={account.id}>
                {account.name}
              </option>
            ))}
          </select>
        </div>

        <div className="flex flex-1 flex-col gap-1.5">
          <Label htmlFor="edit-transactionDate">Data</Label>
          <Input id="edit-transactionDate" type="date" {...register('transactionDate')} />
        </div>
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="edit-categoryId">Categoria</Label>
        <select id="edit-categoryId" className={selectClassName} {...register('categoryId')}>
          <option value="">Sem categoria</option>
          {flatCategories.map((category) => (
            <option key={category.id} value={category.id}>
              {category.label}
            </option>
          ))}
        </select>
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="edit-note">Nota (opcional)</Label>
        <Input id="edit-note" {...register('note')} />
      </div>

      {formError ? <p className="text-sm text-destructive">{formError}</p> : null}

      <div className="flex justify-end gap-2">
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancelar
        </Button>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Salvando...' : 'Salvar alterações'}
        </Button>
      </div>
    </form>
  )
}
