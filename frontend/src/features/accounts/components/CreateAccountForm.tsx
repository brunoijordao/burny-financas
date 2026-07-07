import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { isAxiosError } from 'axios'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { iconOptions } from '@/features/accounts/icons'
import {
  accountTypeLabels,
  accountTypes,
  createAccountSchema,
  type CreateAccountFormValues,
} from '@/features/accounts/schemas'

const selectClassName =
  'flex h-10 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring'

interface CreateAccountFormProps {
  onSubmit: (values: CreateAccountFormValues) => Promise<void>
  onCancel: () => void
}

export function CreateAccountForm({ onSubmit, onCancel }: CreateAccountFormProps) {
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<CreateAccountFormValues>({
    resolver: zodResolver(createAccountSchema),
    defaultValues: {
      name: '',
      icon: iconOptions[0].value,
      color: '#2563eb',
      type: 'CHECKING',
      creditLimit: undefined,
    },
  })

  const isCreditCard = watch('type') === 'CREDIT_CARD'

  const submit = handleSubmit(async (values) => {
    setFormError(null)
    try {
      await onSubmit(values)
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 400) {
        setFormError('Dados inválidos. Verifique os campos e tente novamente.')
      } else {
        setFormError('Não foi possível criar a conta. Tente novamente em instantes.')
      }
    }
  })

  return (
    <form className="flex flex-col gap-4" onSubmit={submit} noValidate>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="name">Nome</Label>
        <Input id="name" placeholder="Ex: Nubank" aria-invalid={Boolean(errors.name)} {...register('name')} />
        {errors.name ? <p className="text-sm text-destructive">{errors.name.message}</p> : null}
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="type">Tipo de conta</Label>
        <select id="type" className={selectClassName} {...register('type')}>
          {accountTypes.map((type) => (
            <option key={type} value={type}>
              {accountTypeLabels[type]}
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

      {isCreditCard ? (
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="creditLimit">Limite do cartão</Label>
          <Input
            id="creditLimit"
            type="number"
            step="0.01"
            placeholder="0,00"
            aria-invalid={Boolean(errors.creditLimit)}
            {...register('creditLimit')}
          />
          {errors.creditLimit ? (
            <p className="text-sm text-destructive">{errors.creditLimit.message}</p>
          ) : null}
        </div>
      ) : null}

      {formError ? <p className="text-sm text-destructive">{formError}</p> : null}

      <div className="flex justify-end gap-2">
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancelar
        </Button>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Criando...' : 'Criar conta'}
        </Button>
      </div>
    </form>
  )
}
