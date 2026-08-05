import { useEffect, useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'

import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { formatDate } from '@/lib/formatters'
import * as settingsApi from '@/features/settings/api/settingsApi'
import { currencyOptions, dateFormatOptions, preferencesSchema, type PreferencesFormValues } from '@/features/settings/schemas'
import { usePreferencesStore } from '@/features/settings/store/preferencesStore'

const selectClassName =
  'flex h-10 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring'

const CURRENCY_LABELS: Record<(typeof currencyOptions)[number], string> = {
  BRL: 'Real brasileiro (R$)',
  USD: 'Dólar americano (US$)',
  EUR: 'Euro (€)',
}

/** A stable reference date used only to render a live preview of each format's output. */
const PREVIEW_DATE = new Date(2026, 11, 31)

export function PreferencesForm() {
  const currency = usePreferencesStore((state) => state.currency)
  const dateFormat = usePreferencesStore((state) => state.dateFormat)
  const setPreferences = usePreferencesStore((state) => state.setPreferences)

  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting, isDirty, isSubmitSuccessful },
  } = useForm<PreferencesFormValues>({
    resolver: zodResolver(preferencesSchema),
    defaultValues: { currency, dateFormat },
  })

  // Keeps the form aligned with the store once AppLayout's initial GET resolves, but only before
  // the user has touched it — avoids stomping on an in-flight edit.
  useEffect(() => {
    if (!isDirty) {
      reset({ currency, dateFormat })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currency, dateFormat])

  const submit = handleSubmit(async (values) => {
    setFormError(null)
    try {
      const updated = await settingsApi.updatePreferences(values)
      setPreferences(updated)
      reset(updated)
    } catch {
      setFormError('Não foi possível salvar suas preferências. Tente novamente em instantes.')
    }
  })

  return (
    <form className="flex flex-col gap-5" onSubmit={submit} noValidate>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="currency">Moeda</Label>
        <select id="currency" className={selectClassName} aria-invalid={Boolean(errors.currency)} {...register('currency')}>
          {currencyOptions.map((option) => (
            <option key={option} value={option}>
              {CURRENCY_LABELS[option]}
            </option>
          ))}
        </select>
        {errors.currency ? <p className="text-sm text-destructive">{errors.currency.message}</p> : null}
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="dateFormat">Formato de data</Label>
        <select
          id="dateFormat"
          className={selectClassName}
          aria-invalid={Boolean(errors.dateFormat)}
          {...register('dateFormat')}
        >
          {dateFormatOptions.map((option) => (
            <option key={option} value={option}>
              {option} — ex: {formatDate(PREVIEW_DATE, option)}
            </option>
          ))}
        </select>
        {errors.dateFormat ? <p className="text-sm text-destructive">{errors.dateFormat.message}</p> : null}
      </div>

      <p className="text-sm text-muted-foreground">
        Essas preferências controlam como valores monetários e datas são exibidos em todo o sistema — dashboard,
        transações, relatórios e demais telas.
      </p>

      {formError ? <p className="text-sm text-destructive">{formError}</p> : null}
      {!formError && isSubmitSuccessful ? <p className="text-sm text-emerald-600 dark:text-emerald-400">Preferências salvas.</p> : null}

      <div className="flex justify-end">
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Salvando...' : 'Salvar preferências'}
        </Button>
      </div>
    </form>
  )
}
