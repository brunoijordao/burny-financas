import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { isAxiosError } from 'axios'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import type { Category } from '@/features/categories/api/categoriesApi'
import { iconOptions } from '@/features/categories/icons'
import { createCategorySchema, type CreateCategoryFormValues } from '@/features/categories/schemas'

const selectClassName =
  'flex h-10 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring'

interface CreateCategoryFormProps {
  topLevelCategories: Category[]
  onSubmit: (values: CreateCategoryFormValues) => Promise<void>
  onCancel: () => void
}

export function CreateCategoryForm({ topLevelCategories, onSubmit, onCancel }: CreateCategoryFormProps) {
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<CreateCategoryFormValues>({
    resolver: zodResolver(createCategorySchema),
    defaultValues: {
      name: '',
      icon: iconOptions[0].value,
      color: '#2563eb',
      parentCategoryId: '',
    },
  })

  const submit = handleSubmit(async (values) => {
    setFormError(null)
    try {
      await onSubmit(values)
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 400) {
        setFormError('Dados inválidos. Verifique os campos e tente novamente.')
      } else if (isAxiosError(error) && error.response?.status === 422) {
        setFormError('Uma subcategoria não pode ser usada como categoria pai.')
      } else {
        setFormError('Não foi possível criar a categoria. Tente novamente em instantes.')
      }
    }
  })

  return (
    <form className="flex flex-col gap-4" onSubmit={submit} noValidate>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="name">Nome</Label>
        <Input id="name" placeholder="Ex: Restaurantes" aria-invalid={Boolean(errors.name)} {...register('name')} />
        {errors.name ? <p className="text-sm text-destructive">{errors.name.message}</p> : null}
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="parentCategoryId">Categoria pai (opcional)</Label>
        <select id="parentCategoryId" className={selectClassName} {...register('parentCategoryId')}>
          <option value="">Nenhuma (categoria de topo)</option>
          {topLevelCategories.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </select>
      </div>

      <div className="flex gap-4">
        <div className="flex flex-1 flex-col gap-1.5">
          <Label htmlFor="icon">Ícone</Label>
          <select id="icon" className={selectClassName} {...register('icon')}>
            {iconOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>

        <div className="flex flex-col gap-1.5">
          <Label htmlFor="color">Cor</Label>
          <input
            id="color"
            type="color"
            className="h-10 w-14 rounded-md border border-input bg-transparent p-1"
            {...register('color')}
          />
        </div>
      </div>

      {formError ? <p className="text-sm text-destructive">{formError}</p> : null}

      <div className="flex justify-end gap-2">
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancelar
        </Button>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Criando...' : 'Criar categoria'}
        </Button>
      </div>
    </form>
  )
}
