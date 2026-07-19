import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { isAxiosError } from 'axios'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { goalSchema, type GoalFormValues } from '@/features/goals/schemas'

interface CreateGoalFormProps {
  initialValues?: GoalFormValues
  onSubmit: (values: GoalFormValues) => Promise<void>
  onCancel: () => void
}

export function CreateGoalForm({ initialValues, onSubmit, onCancel }: CreateGoalFormProps) {
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<GoalFormValues>({
    resolver: zodResolver(goalSchema),
    defaultValues: initialValues ?? { name: '', targetAmount: '', deadline: '' },
  })

  const submit = handleSubmit(async (values) => {
    setFormError(null)
    try {
      await onSubmit(values)
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 400) {
        setFormError('Dados inválidos. Verifique os campos e tente novamente.')
      } else {
        setFormError('Não foi possível salvar a meta. Tente novamente em instantes.')
      }
    }
  })

  return (
    <form className="flex flex-col gap-4" onSubmit={submit} noValidate>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="name">Nome da meta</Label>
        <Input id="name" placeholder="Ex: Viagem" aria-invalid={Boolean(errors.name)} {...register('name')} />
        {errors.name ? <p className="text-sm text-destructive">{errors.name.message}</p> : null}
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="targetAmount">Valor alvo</Label>
        <Input
          id="targetAmount"
          type="number"
          step="0.01"
          placeholder="0,00"
          aria-invalid={Boolean(errors.targetAmount)}
          {...register('targetAmount')}
        />
        {errors.targetAmount ? <p className="text-sm text-destructive">{errors.targetAmount.message}</p> : null}
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="deadline">Prazo</Label>
        <Input id="deadline" type="date" aria-invalid={Boolean(errors.deadline)} {...register('deadline')} />
        {errors.deadline ? <p className="text-sm text-destructive">{errors.deadline.message}</p> : null}
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
