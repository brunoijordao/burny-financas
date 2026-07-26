import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { valuationSchema, type ValuationFormValues } from '@/features/investments/schemas'

interface ValuationFormProps {
  onSubmit: (values: ValuationFormValues) => Promise<void>
  onCancel: () => void
}

/** Every submission adds a new dated snapshot — it never overwrites a prior valuation (design.md Decision 2). */
export function ValuationForm({ onSubmit, onCancel }: ValuationFormProps) {
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ValuationFormValues>({
    resolver: zodResolver(valuationSchema),
    defaultValues: { valueDate: new Date().toISOString().slice(0, 10), totalValue: '' },
  })

  const submit = handleSubmit(async (values) => {
    setFormError(null)
    try {
      await onSubmit(values)
    } catch {
      setFormError('Não foi possível registrar o valor atual. Tente novamente em instantes.')
    }
  })

  return (
    <form className="flex flex-col gap-4" onSubmit={submit} noValidate>
      <div className="flex gap-4">
        <div className="flex flex-1 flex-col gap-1.5">
          <Label htmlFor="valueDate">Data</Label>
          <Input id="valueDate" type="date" aria-invalid={Boolean(errors.valueDate)} {...register('valueDate')} />
          {errors.valueDate ? <p className="text-sm text-destructive">{errors.valueDate.message}</p> : null}
        </div>

        <div className="flex flex-1 flex-col gap-1.5">
          <Label htmlFor="totalValue">Valor de mercado atual</Label>
          <Input
            id="totalValue"
            type="number"
            step="0.01"
            placeholder="0,00"
            aria-invalid={Boolean(errors.totalValue)}
            {...register('totalValue')}
          />
          {errors.totalValue ? <p className="text-sm text-destructive">{errors.totalValue.message}</p> : null}
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
