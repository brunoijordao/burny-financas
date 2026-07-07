import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { isAxiosError } from 'axios'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import type { Account } from '@/features/accounts/api/accountsApi'
import type { Category } from '@/features/categories/api/categoriesApi'
import {
  createTransactionSchema,
  recurrenceFrequencies,
  recurrenceFrequencyLabels,
  transactionTypeLabels,
  transactionTypes,
  type CreateTransactionFormValues,
} from '@/features/transactions/schemas'

const selectClassName =
  'flex h-10 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring'

function flattenCategories(categories: Category[]): { id: number; label: string }[] {
  return categories.flatMap((category) => [
    { id: category.id, label: category.name },
    ...category.subcategories.map((sub) => ({ id: sub.id, label: `— ${sub.name}` })),
  ])
}

interface CreateTransactionFormProps {
  accounts: Account[]
  categories: Category[]
  onSubmit: (values: CreateTransactionFormValues) => Promise<void>
  onCancel: () => void
}

export function CreateTransactionForm({ accounts, categories, onSubmit, onCancel }: CreateTransactionFormProps) {
  const [formError, setFormError] = useState<string | null>(null)
  const flatCategories = flattenCategories(categories)

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<CreateTransactionFormValues>({
    resolver: zodResolver(createTransactionSchema),
    defaultValues: {
      description: '',
      amount: '',
      type: 'EXPENSE',
      transactionDate: new Date().toISOString().slice(0, 10),
      accountId: accounts[0] ? String(accounts[0].id) : '',
      categoryId: '',
      note: '',
      recurring: false,
      frequency: undefined,
      startDate: '',
      endDate: '',
    },
  })

  const isRecurring = watch('recurring')

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
        setFormError('Não foi possível criar a transação. Tente novamente em instantes.')
      }
    }
  })

  return (
    <form className="flex flex-col gap-4" onSubmit={submit} noValidate>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="description">Descrição</Label>
        <Input
          id="description"
          placeholder="Ex: Supermercado"
          aria-invalid={Boolean(errors.description)}
          {...register('description')}
        />
        {errors.description ? <p className="text-sm text-destructive">{errors.description.message}</p> : null}
      </div>

      <div className="flex gap-4">
        <div className="flex flex-1 flex-col gap-1.5">
          <Label htmlFor="type">Tipo</Label>
          <select id="type" className={selectClassName} {...register('type')}>
            {transactionTypes.map((type) => (
              <option key={type} value={type}>
                {transactionTypeLabels[type]}
              </option>
            ))}
          </select>
        </div>

        <div className="flex flex-1 flex-col gap-1.5">
          <Label htmlFor="amount">Valor</Label>
          <Input
            id="amount"
            type="number"
            step="0.01"
            placeholder="0,00"
            aria-invalid={Boolean(errors.amount)}
            {...register('amount')}
          />
          {errors.amount ? <p className="text-sm text-destructive">{errors.amount.message}</p> : null}
        </div>
      </div>

      <div className="flex gap-4">
        <div className="flex flex-1 flex-col gap-1.5">
          <Label htmlFor="accountId">Conta</Label>
          <select id="accountId" className={selectClassName} {...register('accountId')}>
            {accounts.map((account) => (
              <option key={account.id} value={account.id}>
                {account.name}
              </option>
            ))}
          </select>
          {errors.accountId ? <p className="text-sm text-destructive">{errors.accountId.message}</p> : null}
        </div>

        <div className="flex flex-1 flex-col gap-1.5">
          <Label htmlFor="transactionDate">Data</Label>
          <Input
            id="transactionDate"
            type="date"
            aria-invalid={Boolean(errors.transactionDate)}
            {...register('transactionDate')}
          />
        </div>
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="categoryId">Categoria</Label>
        <select id="categoryId" className={selectClassName} {...register('categoryId')}>
          <option value="">Detectar automaticamente pela descrição</option>
          {flatCategories.map((category) => (
            <option key={category.id} value={category.id}>
              {category.label}
            </option>
          ))}
        </select>
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="note">Nota (opcional)</Label>
        <Input id="note" placeholder="Detalhes adicionais" {...register('note')} />
      </div>

      <div className="flex items-center gap-2">
        <input id="recurring" type="checkbox" className="size-4" {...register('recurring')} />
        <Label htmlFor="recurring">Transação recorrente</Label>
      </div>

      {isRecurring ? (
        <div className="flex flex-col gap-4 rounded-md border border-input p-4">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="frequency">Frequência</Label>
            <select id="frequency" className={selectClassName} {...register('frequency')}>
              <option value="">Selecione</option>
              {recurrenceFrequencies.map((frequency) => (
                <option key={frequency} value={frequency}>
                  {recurrenceFrequencyLabels[frequency]}
                </option>
              ))}
            </select>
            {errors.frequency ? <p className="text-sm text-destructive">{errors.frequency.message}</p> : null}
          </div>

          <div className="flex gap-4">
            <div className="flex flex-1 flex-col gap-1.5">
              <Label htmlFor="startDate">Início</Label>
              <Input id="startDate" type="date" {...register('startDate')} />
              {errors.startDate ? <p className="text-sm text-destructive">{errors.startDate.message}</p> : null}
            </div>
            <div className="flex flex-1 flex-col gap-1.5">
              <Label htmlFor="endDate">Fim (opcional)</Label>
              <Input id="endDate" type="date" {...register('endDate')} />
            </div>
          </div>
        </div>
      ) : null}

      {formError ? <p className="text-sm text-destructive">{formError}</p> : null}

      <div className="flex justify-end gap-2">
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancelar
        </Button>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Criando...' : 'Criar transação'}
        </Button>
      </div>
    </form>
  )
}
