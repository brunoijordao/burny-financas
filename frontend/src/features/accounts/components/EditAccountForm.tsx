import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { isAxiosError } from 'axios'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import type { Account } from '@/features/accounts/api/accountsApi'
import { iconOptions } from '@/features/accounts/icons'
import { editAccountSchema, type EditAccountFormValues } from '@/features/accounts/schemas'

const selectClassName =
  'flex h-10 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring'

interface EditAccountFormProps {
  account: Account
  onSubmit: (values: EditAccountFormValues) => Promise<void>
  onCancel: () => void
}

export function EditAccountForm({ account, onSubmit, onCancel }: EditAccountFormProps) {
  const [formError, setFormError] = useState<string | null>(null)
  const isCreditCard = account.type === 'CREDIT_CARD'

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<EditAccountFormValues>({
    resolver: zodResolver(editAccountSchema),
    defaultValues: {
      name: account.name,
      icon: account.icon,
      color: account.color,
      creditLimit: account.creditLimit != null ? String(account.creditLimit) : undefined,
    },
  })

  const submit = handleSubmit(async (values) => {
    setFormError(null)
    try {
      await onSubmit(values)
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 400) {
        setFormError('Dados inválidos. Verifique os campos e tente novamente.')
      } else {
        setFormError('Não foi possível salvar as alterações. Tente novamente em instantes.')
      }
    }
  })

  return (
    <form className="flex flex-col gap-4" onSubmit={submit} noValidate>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="edit-name">Nome</Label>
        <Input id="edit-name" aria-invalid={Boolean(errors.name)} {...register('name')} />
        {errors.name ? <p className="text-sm text-destructive">{errors.name.message}</p> : null}
      </div>

      <div className="flex gap-4">
        <div className="flex flex-1 flex-col gap-1.5">
          <Label htmlFor="edit-icon">Ícone</Label>
          <select id="edit-icon" className={selectClassName} {...register('icon')}>
            {iconOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>

        <div className="flex flex-col gap-1.5">
          <Label htmlFor="edit-color">Cor</Label>
          <input
            id="edit-color"
            type="color"
            className="h-10 w-14 rounded-md border border-input bg-transparent p-1"
            {...register('color')}
          />
        </div>
      </div>

      {isCreditCard ? (
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="edit-creditLimit">Limite do cartão</Label>
          <Input
            id="edit-creditLimit"
            type="number"
            step="0.01"
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
          {isSubmitting ? 'Salvando...' : 'Salvar alterações'}
        </Button>
      </div>
    </form>
  )
}
