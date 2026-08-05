import type { CurrencyCode, DateFormatCode } from '@/features/settings/api/settingsApi'

export type { CurrencyCode, DateFormatCode }

/**
 * Locale stays fixed at `pt-BR` (surrounding UI text is Portuguese; full i18n is out of scope —
 * see design.md Non-Goals). Only the `currency` code changes, which is enough to change the
 * symbol/ISO formatting (e.g. `US$ 1.234,56` vs `R$ 1.234,56`).
 */
export function formatCurrency(value: number, currency: CurrencyCode): string {
  return value.toLocaleString('pt-BR', { style: 'currency', currency })
}

const DATE_ONLY_PATTERN = /^\d{4}-\d{2}-\d{2}$/

/**
 * Date-only strings (`YYYY-MM-DD`) are parsed as UTC midnight by the `Date` constructor, which
 * shifts a day backward once rendered in a negative-UTC-offset timezone. Anchoring to local
 * midnight avoids that — the same workaround previously duplicated ad hoc across components
 * (e.g. `GoalProgressCard.tsx`).
 */
function toLocalDate(value: string | Date): Date {
  if (value instanceof Date) {
    return value
  }
  return new Date(DATE_ONLY_PATTERN.test(value) ? `${value}T00:00:00` : value)
}

/**
 * Manual `DD`/`MM`/`YYYY` token substitution: `Intl.DateTimeFormat` doesn't map cleanly onto
 * arbitrary slash/dash-delimited token orders, and only 3 fixed patterns are supported (see
 * design.md Decision 3), so a small dependency-free formatter is simpler than a date library.
 */
export function formatDate(value: string | Date, format: DateFormatCode): string {
  const date = toLocalDate(value)
  const day = String(date.getDate()).padStart(2, '0')
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const year = String(date.getFullYear())

  switch (format) {
    case 'MM/DD/YYYY':
      return `${month}/${day}/${year}`
    case 'YYYY-MM-DD':
      return `${year}-${month}-${day}`
    case 'DD/MM/YYYY':
    default:
      return `${day}/${month}/${year}`
  }
}
