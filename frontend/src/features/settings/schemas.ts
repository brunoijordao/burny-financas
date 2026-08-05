import { z } from 'zod'

/** Mirrors the backend's closed sets (`CurrencyCode`, `DateFormatCode`) — see design.md Decision 2. */
export const currencyOptions = ['BRL', 'USD', 'EUR'] as const
export const dateFormatOptions = ['DD/MM/YYYY', 'MM/DD/YYYY', 'YYYY-MM-DD'] as const

export const preferencesSchema = z.object({
  currency: z.enum(currencyOptions),
  dateFormat: z.enum(dateFormatOptions),
})

export type PreferencesFormValues = z.infer<typeof preferencesSchema>
