import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { isAxiosError } from 'axios'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import type { Category } from '@/features/categories/api/categoriesApi'
import type { PdfImportItem } from '@/features/pdf-import/api/pdfImportApi'
import {
  editPdfImportItemSchema,
  type EditPdfImportItemFormValues,
} from '@/features/pdf-import/schemas'
import { transactionTypeLabels, transactionTypes } from '@/features/transactions/schemas'

const selectClassName =
  'flex h-10 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring'

function formatCurrency(value: number) {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

function flattenCategories(categories: Category[]): { id: number; label: string }[] {
  return categories.flatMap((category) => [
    { id: category.id, label: category.name },
    ...category.subcategories.map((sub) => ({ id: sub.id, label: `— ${sub.name}` })),
  ])
}

interface PdfImportItemRowProps {
  item: PdfImportItem
  categories: Category[]
  onSave: (values: EditPdfImportItemFormValues) => Promise<void>
  onDiscard: () => Promise<void>
  onConfirm: () => Promise<void>
}

export function PdfImportItemRow({ item, categories, onSave, onDiscard, onConfirm }: PdfImportItemRowProps) {
  const [editing, setEditing] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const flatCategories = flattenCategories(categories)
  const categoryName = item.categoryId != null
    ? flatCategories.find((category) => category.id === item.categoryId)?.label
    : null

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<EditPdfImportItemFormValues>({
    resolver: zodResolver(editPdfImportItemSchema),
    defaultValues: {
      transactionDate: item.transactionDate,
      description: item.description,
      amount: String(item.amount),
      type: item.type,
      categoryId: item.categoryId != null ? String(item.categoryId) : '',
    },
  })

  const submitEdit = handleSubmit(async (values) => {
    try {
      await onSave(values)
      setEditing(false)
    } catch {
      setError('Não foi possível salvar as alterações.')
    }
  })

  const runAction = async (action: () => Promise<void>, failureMessage: string) => {
    setBusy(true)
    setError(null)
    try {
      await action()
    } catch (actionError) {
      if (isAxiosError(actionError) && actionError.response?.status === 409) {
        setError('Este item já foi confirmado ou descartado.')
      } else {
        setError(failureMessage)
      }
    } finally {
      setBusy(false)
    }
  }

  if (item.status !== 'PENDING') {
    return (
      <div className="flex items-center justify-between gap-3 rounded-md border border-input px-3 py-2 text-sm opacity-70">
        <div className="flex flex-col">
          <span>{item.description}</span>
          <span className="text-xs text-muted-foreground">
            {item.transactionDate} · {item.status === 'CONFIRMED' ? 'Confirmada' : 'Descartada'}
          </span>
        </div>
        <span className={item.type === 'EXPENSE' ? 'font-semibold text-destructive' : 'font-semibold text-emerald-600 dark:text-emerald-400'}>
          {item.type === 'EXPENSE' ? '-' : '+'}
          {formatCurrency(item.amount)}
        </span>
      </div>
    )
  }

  if (editing) {
    return (
      <form className="flex flex-col gap-3 rounded-md border border-input p-3" onSubmit={submitEdit} noValidate>
        <div className="flex gap-3">
          <div className="flex flex-1 flex-col gap-1.5">
            <Label htmlFor={`desc-${item.id}`}>Descrição</Label>
            <Input id={`desc-${item.id}`} aria-invalid={Boolean(errors.description)} {...register('description')} />
          </div>
          <div className="flex flex-1 flex-col gap-1.5">
            <Label htmlFor={`date-${item.id}`}>Data</Label>
            <Input id={`date-${item.id}`} type="date" {...register('transactionDate')} />
          </div>
        </div>
        <div className="flex gap-3">
          <div className="flex flex-1 flex-col gap-1.5">
            <Label htmlFor={`type-${item.id}`}>Tipo</Label>
            <select id={`type-${item.id}`} className={selectClassName} {...register('type')}>
              {transactionTypes.map((type) => (
                <option key={type} value={type}>
                  {transactionTypeLabels[type]}
                </option>
              ))}
            </select>
          </div>
          <div className="flex flex-1 flex-col gap-1.5">
            <Label htmlFor={`amount-${item.id}`}>Valor</Label>
            <Input id={`amount-${item.id}`} type="number" step="0.01" aria-invalid={Boolean(errors.amount)} {...register('amount')} />
            {errors.amount ? <p className="text-sm text-destructive">{errors.amount.message}</p> : null}
          </div>
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor={`category-${item.id}`}>Categoria</Label>
          <select id={`category-${item.id}`} className={selectClassName} {...register('categoryId')}>
            <option value="">Sem categoria</option>
            {flatCategories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.label}
              </option>
            ))}
          </select>
        </div>
        {error ? <p className="text-sm text-destructive">{error}</p> : null}
        <div className="flex justify-end gap-2">
          <Button type="button" variant="outline" size="sm" onClick={() => setEditing(false)} disabled={isSubmitting}>
            Cancelar
          </Button>
          <Button type="submit" size="sm" disabled={isSubmitting}>
            {isSubmitting ? 'Salvando...' : 'Salvar'}
          </Button>
        </div>
      </form>
    )
  }

  return (
    <div className="flex flex-col gap-2 rounded-md border border-input px-3 py-2 text-sm">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-col">
          <span className="font-medium">{item.description}</span>
          <span className="text-xs text-muted-foreground">
            {item.transactionDate}
            {categoryName ? ` · ${categoryName}` : ' · Sem categoria'}
          </span>
        </div>
        <div className="flex items-center gap-3">
          <span className={item.type === 'EXPENSE' ? 'font-semibold text-destructive' : 'font-semibold text-emerald-600 dark:text-emerald-400'}>
            {item.type === 'EXPENSE' ? '-' : '+'}
            {formatCurrency(item.amount)}
          </span>
          <div className="flex gap-1">
            <Button type="button" variant="outline" size="sm" disabled={busy} onClick={() => setEditing(true)}>
              Editar
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              disabled={busy}
              onClick={() => void runAction(onDiscard, 'Não foi possível remover este item.')}
            >
              Remover
            </Button>
            <Button
              type="button"
              size="sm"
              disabled={busy}
              onClick={() => void runAction(onConfirm, 'Não foi possível confirmar este item.')}
            >
              Confirmar
            </Button>
          </div>
        </div>
      </div>
      {error ? <p className="text-sm text-destructive">{error}</p> : null}
    </div>
  )
}
