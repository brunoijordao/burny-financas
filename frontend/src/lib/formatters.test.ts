import { describe, expect, it } from 'vitest'

import { formatCurrency, formatDate } from './formatters'

describe('formatCurrency', () => {
  it('formats BRL with the Real symbol', () => {
    expect(formatCurrency(1234.5, 'BRL')).toBe('R$\xa01.234,50')
  })

  it('formats USD with the Dollar symbol', () => {
    expect(formatCurrency(1234.5, 'USD')).toBe('US$\xa01.234,50')
  })

  it('formats EUR with the Euro symbol', () => {
    expect(formatCurrency(1234.5, 'EUR')).toBe('€\xa01.234,50')
  })
})

describe('formatDate', () => {
  it('formats a date-only string as DD/MM/YYYY', () => {
    expect(formatDate('2026-08-04', 'DD/MM/YYYY')).toBe('04/08/2026')
  })

  it('formats a date-only string as MM/DD/YYYY', () => {
    expect(formatDate('2026-08-04', 'MM/DD/YYYY')).toBe('08/04/2026')
  })

  it('formats a date-only string as YYYY-MM-DD', () => {
    expect(formatDate('2026-08-04', 'YYYY-MM-DD')).toBe('2026-08-04')
  })

  it('zero-pads single-digit day and month', () => {
    expect(formatDate('2026-01-05', 'DD/MM/YYYY')).toBe('05/01/2026')
  })

  it('does not shift a date-only string to the previous day in a negative-UTC-offset timezone', () => {
    // Regression check for the UTC-midnight parsing pitfall the formatter guards against.
    expect(formatDate('2026-01-01', 'DD/MM/YYYY')).toBe('01/01/2026')
  })

  it('accepts a Date object directly', () => {
    expect(formatDate(new Date(2026, 7, 4), 'DD/MM/YYYY')).toBe('04/08/2026')
  })
})
