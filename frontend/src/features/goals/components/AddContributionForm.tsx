import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { contributionSchema, type ContributionFormValues } from '@/features/goals/schemas'

interface AddContributionFormProps {
  onSubmit: (amount: number, contributionDate?: string) => Promise<void>
  onCancel: () => void
}

export function AddContributionForm({ onSubmit, onCancel }: AddContributionFormProps) {
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ContributionFormValues>({
    resolver: zodResolver(contributionSchema),
    defaultValues: { amount: '', contributionDate: '' },
  })

  const submit = handleSubmit(async (values) => {
    setFormError(null)
    try {
      await onSubmit(Number(values.amount), values.contributionDate || undefined)
    } catch {
      setFormError('Não foi possível registrar o aporte. Tente novamente em instantes.')
    }
  })

  return (
    <form className="flex flex-col gap-4" onSubmit={submit} noValidate>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="amount">Valor do aporte</Label>
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

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="contributionDate">Data (opcional, padrão hoje)</Label>
        <Input id="contributionDate" type="date" {...register('contributionDate')} />
      </div>

      {formError ? <p className="text-sm text-destructive">{formError}</p> : null}

      <div className="flex justify-end gap-2">
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancelar
        </Button>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Registrando...' : 'Registrar aporte'}
        </Button>
      </div>
    </form>
  )
}
