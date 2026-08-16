import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { isAxiosError } from 'axios'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { selectFieldClassName as selectClassName } from '@/components/ui/select-field'
import type { Account } from '@/features/accounts/api/accountsApi'
import type { Category } from '@/features/categories/api/categoriesApi'
import { planningEntrySchema, type PlanningEntryFormValues } from '@/features/planning/schemas'

const typeLabels: Record<PlanningEntryFormValues['type'], string> = {
  PAYABLE: 'A pagar',
  RECEIVABLE: 'A receber',
}

function flattenCategories(categories: Category[]): { id: number; label: string }[] {
  return categories.flatMap((category) => [
    { id: category.id, label: category.name },
    ...category.subcategories.map((sub) => ({ id: sub.id, label: `— ${sub.name}` })),
  ])
}

interface PlanningEntryFormProps {
  accounts: Account[]
  categories: Category[]
  initialValues?: PlanningEntryFormValues
  onSubmit: (values: PlanningEntryFormValues) => Promise<void>
  onCancel: () => void
}

export function PlanningEntryForm({ accounts, categories, initialValues, onSubmit, onCancel }: PlanningEntryFormProps) {
  const [formError, setFormError] = useState<string | null>(null)
  const flatCategories = flattenCategories(categories)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<PlanningEntryFormValues>({
    resolver: zodResolver(planningEntrySchema),
    defaultValues: initialValues ?? {
      type: 'PAYABLE',
      accountId: accounts[0] ? String(accounts[0].id) : '',
      categoryId: '',
      amount: '',
      description: '',
      dueDate: new Date().toISOString().slice(0, 10),
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
        setFormError('Não foi possível salvar o lançamento. Tente novamente em instantes.')
      }
    }
  })

  return (
    <form className="flex flex-col gap-4" onSubmit={submit} noValidate>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="description">Descrição</Label>
        <Input
          id="description"
          placeholder="Ex: Conta de luz"
          aria-invalid={Boolean(errors.description)}
          {...register('description')}
        />
        {errors.description ? <p className="text-sm text-destructive">{errors.description.message}</p> : null}
      </div>

      <div className="flex gap-4">
        <div className="flex flex-1 flex-col gap-1.5">
          <Label htmlFor="type">Tipo</Label>
          <select id="type" className={selectClassName} {...register('type')}>
            {(Object.keys(typeLabels) as Array<PlanningEntryFormValues['type']>).map((type) => (
              <option key={type} value={type}>
                {typeLabels[type]}
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
          <Label htmlFor="dueDate">Vencimento</Label>
          <Input id="dueDate" type="date" aria-invalid={Boolean(errors.dueDate)} {...register('dueDate')} />
          {errors.dueDate ? <p className="text-sm text-destructive">{errors.dueDate.message}</p> : null}
        </div>
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="categoryId">Categoria (opcional)</Label>
        <select id="categoryId" className={selectClassName} {...register('categoryId')}>
          <option value="">Sem categoria</option>
          {flatCategories.map((category) => (
            <option key={category.id} value={category.id}>
              {category.label}
            </option>
          ))}
        </select>
      </div>

      {formError ? <p className="text-sm text-destructive">{formError}</p> : null}

      <div className="flex justify-end gap-2">
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancelar
        </Button>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Salvando...' : 'Salvar'}
        </Button>
      </div>
    </form>
  )
}
