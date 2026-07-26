import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { isAxiosError } from 'axios'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import type { Account } from '@/features/accounts/api/accountsApi'
import { assetSchema, type AssetFormValues } from '@/features/investments/schemas'

const selectClassName =
  'flex h-10 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring'

const typeLabels: Record<AssetFormValues['type'], string> = {
  STOCK: 'Ação',
  FII: 'FII',
  CDB: 'CDB',
  TREASURY_DIRECT: 'Tesouro Direto',
  CRYPTO: 'Criptomoeda',
}

interface AssetFormProps {
  accounts: Account[]
  initialValues?: AssetFormValues
  onSubmit: (values: AssetFormValues) => Promise<void>
  onCancel: () => void
}

export function AssetForm({ accounts, initialValues, onSubmit, onCancel }: AssetFormProps) {
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<AssetFormValues>({
    resolver: zodResolver(assetSchema),
    defaultValues: initialValues ?? { name: '', ticker: '', type: 'STOCK', accountId: '' },
  })

  const submit = handleSubmit(async (values) => {
    setFormError(null)
    try {
      await onSubmit(values)
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 400) {
        setFormError('Dados inválidos. Verifique os campos e tente novamente.')
      } else {
        setFormError('Não foi possível salvar o ativo. Tente novamente em instantes.')
      }
    }
  })

  return (
    <form className="flex flex-col gap-4" onSubmit={submit} noValidate>
      <div className="flex gap-4">
        <div className="flex flex-1 flex-col gap-1.5">
          <Label htmlFor="name">Nome</Label>
          <Input id="name" placeholder="Ex: PETR4" aria-invalid={Boolean(errors.name)} {...register('name')} />
          {errors.name ? <p className="text-sm text-destructive">{errors.name.message}</p> : null}
        </div>

        <div className="flex flex-1 flex-col gap-1.5">
          <Label htmlFor="ticker">Ticker/código (opcional)</Label>
          <Input id="ticker" placeholder="Ex: PETR4" {...register('ticker')} />
        </div>
      </div>

      <div className="flex gap-4">
        <div className="flex flex-1 flex-col gap-1.5">
          <Label htmlFor="type">Tipo</Label>
          <select id="type" className={selectClassName} {...register('type')}>
            {(Object.keys(typeLabels) as Array<AssetFormValues['type']>).map((type) => (
              <option key={type} value={type}>
                {typeLabels[type]}
              </option>
            ))}
          </select>
        </div>

        <div className="flex flex-1 flex-col gap-1.5">
          <Label htmlFor="accountId">Conta/corretora (opcional)</Label>
          <select id="accountId" className={selectClassName} {...register('accountId')}>
            <option value="">Sem conta vinculada</option>
            {accounts.map((account) => (
              <option key={account.id} value={account.id}>
                {account.name}
              </option>
            ))}
          </select>
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
