import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { isAxiosError } from 'axios'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import type { Category } from '@/features/categories/api/categoriesApi'
import { setBudgetSchema, type SetBudgetFormValues } from '@/features/budgets/schemas'

const selectClassName =
  'flex h-10 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring'

function flattenCategories(categories: Category[]): { id: number; label: string }[] {
  return categories.flatMap((category) => [
    { id: category.id, label: category.name },
    ...category.subcategories.map((sub) => ({ id: sub.id, label: `— ${sub.name}` })),
  ])
}

interface SetBudgetFormProps {
  categories: Category[]
  initialCategoryId?: number
  initialLimitAmount?: number
  /** When set, the category is fixed (editing an existing budget) and the picker is hidden. */
  lockCategory?: boolean
  onSubmit: (categoryId: number, limitAmount: number) => Promise<void>
  onCancel: () => void
}

export function SetBudgetForm({
  categories,
  initialCategoryId,
  initialLimitAmount,
  lockCategory,
  onSubmit,
  onCancel,
}: SetBudgetFormProps) {
  const [formError, setFormError] = useState<string | null>(null)
  const flatCategories = flattenCategories(categories)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<SetBudgetFormValues>({
    resolver: zodResolver(setBudgetSchema),
    defaultValues: {
      categoryId: initialCategoryId ? String(initialCategoryId) : '',
      limitAmount: initialLimitAmount !== undefined ? String(initialLimitAmount) : '',
    },
  })

  const submit = handleSubmit(async (values) => {
    setFormError(null)
    try {
      await onSubmit(Number(values.categoryId), Number(values.limitAmount))
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 400) {
        setFormError('Dados inválidos. Verifique os campos e tente novamente.')
      } else if (isAxiosError(error) && error.response?.status === 404) {
        setFormError('Categoria não encontrada.')
      } else {
        setFormError('Não foi possível salvar o orçamento. Tente novamente em instantes.')
      }
    }
  })

  return (
    <form className="flex flex-col gap-4" onSubmit={submit} noValidate>
      {!lockCategory && (
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="categoryId">Categoria</Label>
          <select
            id="categoryId"
            className={selectClassName}
            aria-invalid={Boolean(errors.categoryId)}
            {...register('categoryId')}
          >
            <option value="">Selecione...</option>
            {flatCategories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.label}
              </option>
            ))}
          </select>
          {errors.categoryId ? <p className="text-sm text-destructive">{errors.categoryId.message}</p> : null}
        </div>
      )}

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="limitAmount">Limite mensal</Label>
        <Input
          id="limitAmount"
          type="number"
          step="0.01"
          placeholder="0,00"
          aria-invalid={Boolean(errors.limitAmount)}
          {...register('limitAmount')}
        />
        {errors.limitAmount ? <p className="text-sm text-destructive">{errors.limitAmount.message}</p> : null}
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
