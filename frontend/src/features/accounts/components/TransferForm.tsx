import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { isAxiosError } from 'axios'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import type { Account } from '@/features/accounts/api/accountsApi'
import { transferSchema, type TransferFormValues } from '@/features/accounts/schemas'

const selectClassName =
  'flex h-10 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring'

interface TransferFormProps {
  accounts: Account[]
  onSubmit: (values: TransferFormValues) => Promise<void>
  onCancel: () => void
}

/**
 * Mirrors the backend's transfer rules client-side for a fast first pass (credit card accounts
 * can't be a transfer source); the backend remains the source of truth and is the one that
 * actually rejects insufficient balance, inactive accounts, etc.
 */
export function TransferForm({ accounts, onSubmit, onCancel }: TransferFormProps) {
  const [formError, setFormError] = useState<string | null>(null)
  const sourceCandidates = accounts.filter((account) => account.type !== 'CREDIT_CARD')

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<TransferFormValues>({
    resolver: zodResolver(transferSchema),
    defaultValues: { sourceAccountId: '', destinationAccountId: '', amount: '' },
  })

  const submit = handleSubmit(async (values) => {
    setFormError(null)
    try {
      await onSubmit(values)
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 422) {
        setFormError('Transferência não permitida: saldo insuficiente ou conta inválida para esta operação.')
      } else if (isAxiosError(error) && error.response?.status === 404) {
        setFormError('Conta de origem ou destino não encontrada.')
      } else {
        setFormError('Não foi possível concluir a transferência. Tente novamente em instantes.')
      }
    }
  })

  return (
    <form className="flex flex-col gap-4" onSubmit={submit} noValidate>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="sourceAccountId">Conta de origem</Label>
        <select id="sourceAccountId" className={selectClassName} {...register('sourceAccountId')}>
          <option value="">Selecione...</option>
          {sourceCandidates.map((account) => (
            <option key={account.id} value={account.id}>
              {account.name}
            </option>
          ))}
        </select>
        {errors.sourceAccountId ? (
          <p className="text-sm text-destructive">{errors.sourceAccountId.message}</p>
        ) : null}
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="destinationAccountId">Conta de destino</Label>
        <select id="destinationAccountId" className={selectClassName} {...register('destinationAccountId')}>
          <option value="">Selecione...</option>
          {accounts.map((account) => (
            <option key={account.id} value={account.id}>
              {account.name}
              {account.type === 'CREDIT_CARD' ? ' (pagamento de fatura)' : ''}
            </option>
          ))}
        </select>
        {errors.destinationAccountId ? (
          <p className="text-sm text-destructive">{errors.destinationAccountId.message}</p>
        ) : null}
      </div>

      <div className="flex flex-col gap-1.5">
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

      {formError ? <p className="text-sm text-destructive">{formError}</p> : null}

      <div className="flex justify-end gap-2">
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancelar
        </Button>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Transferindo...' : 'Transferir'}
        </Button>
      </div>
    </form>
  )
}
