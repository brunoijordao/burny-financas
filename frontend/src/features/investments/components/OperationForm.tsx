import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { isAxiosError } from 'axios'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { operationSchema, type OperationFormValues } from '@/features/investments/schemas'

const selectClassName =
  'flex h-10 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring'

const typeLabels: Record<OperationFormValues['type'], string> = {
  BUY: 'Aporte (compra)',
  SELL: 'Resgate (venda)',
}

interface OperationFormProps {
  onSubmit: (values: OperationFormValues) => Promise<void>
  onCancel: () => void
}

/** Sell-exceeding-quantity is rejected server-side (400); the message here surfaces that case explicitly. */
export function OperationForm({ onSubmit, onCancel }: OperationFormProps) {
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<OperationFormValues>({
    resolver: zodResolver(operationSchema),
    defaultValues: {
      type: 'BUY',
      quantity: '',
      unitPrice: '',
      operationDate: new Date().toISOString().slice(0, 10),
    },
  })

  const submit = handleSubmit(async (values) => {
    setFormError(null)
    try {
      await onSubmit(values)
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 400) {
        setFormError(
          error.response.data?.message?.includes('exceeds')
            ? 'A quantidade do resgate é maior do que a quantidade atual do ativo.'
            : 'Dados inválidos. Verifique os campos e tente novamente.',
        )
      } else {
        setFormError('Não foi possível registrar a operação. Tente novamente em instantes.')
      }
    }
  })

  return (
    <form className="flex flex-col gap-4" onSubmit={submit} noValidate>
      <div className="flex gap-4">
        <div className="flex flex-1 flex-col gap-1.5">
          <Label htmlFor="type">Tipo</Label>
          <select id="type" className={selectClassName} {...register('type')}>
            {(Object.keys(typeLabels) as Array<OperationFormValues['type']>).map((type) => (
              <option key={type} value={type}>
                {typeLabels[type]}
              </option>
            ))}
          </select>
        </div>

        <div className="flex flex-1 flex-col gap-1.5">
          <Label htmlFor="operationDate">Data</Label>
          <Input
            id="operationDate"
            type="date"
            aria-invalid={Boolean(errors.operationDate)}
            {...register('operationDate')}
          />
          {errors.operationDate ? <p className="text-sm text-destructive">{errors.operationDate.message}</p> : null}
        </div>
      </div>

      <div className="flex gap-4">
        <div className="flex flex-1 flex-col gap-1.5">
          <Label htmlFor="quantity">Quantidade</Label>
          <Input
            id="quantity"
            type="number"
            step="0.00000001"
            placeholder="0"
            aria-invalid={Boolean(errors.quantity)}
            {...register('quantity')}
          />
          {errors.quantity ? <p className="text-sm text-destructive">{errors.quantity.message}</p> : null}
        </div>

        <div className="flex flex-1 flex-col gap-1.5">
          <Label htmlFor="unitPrice">Preço unitário</Label>
          <Input
            id="unitPrice"
            type="number"
            step="0.01"
            placeholder="0,00"
            aria-invalid={Boolean(errors.unitPrice)}
            {...register('unitPrice')}
          />
          {errors.unitPrice ? <p className="text-sm text-destructive">{errors.unitPrice.message}</p> : null}
        </div>
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
