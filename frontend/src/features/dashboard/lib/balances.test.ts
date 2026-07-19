import { describe, expect, it } from 'vitest'

import type { Account } from '@/features/accounts/api/accountsApi'
import { computeAvailableBalance, computeProjectedBalance, computeTotalInvoice } from './balances'

function makeAccount(overrides: Partial<Account>): Account {
  return {
    id: 1,
    name: 'Conta',
    icon: 'wallet',
    color: '#123456',
    type: 'CHECKING',
    active: true,
    balance: 0,
    creditLimit: null,
    currentInvoice: null,
    createdAt: '2026-01-01T00:00:00',
    updatedAt: '2026-01-01T00:00:00',
    ...overrides,
  }
}

describe('computeAvailableBalance', () => {
  it('sums balances of active non-credit-card accounts and excludes credit card invoices', () => {
    const accounts = [
      makeAccount({ id: 1, type: 'CHECKING', balance: 1000 }),
      makeAccount({ id: 2, type: 'SAVINGS', balance: 500 }),
      makeAccount({ id: 3, type: 'CREDIT_CARD', balance: null, currentInvoice: 300 }),
    ]

    expect(computeAvailableBalance(accounts)).toBe(1500)
  })

  it('excludes inactive accounts', () => {
    const accounts = [
      makeAccount({ id: 1, type: 'CHECKING', balance: 1000 }),
      makeAccount({ id: 2, type: 'CHECKING', balance: 500, active: false }),
    ]

    expect(computeAvailableBalance(accounts)).toBe(1000)
  })

  it('returns zero when every account is a credit card', () => {
    const accounts = [makeAccount({ type: 'CREDIT_CARD', balance: null, currentInvoice: 200 })]

    expect(computeAvailableBalance(accounts)).toBe(0)
  })

  it('sums every account when none are credit cards', () => {
    const accounts = [
      makeAccount({ id: 1, type: 'CHECKING', balance: 100 }),
      makeAccount({ id: 2, type: 'PHYSICAL_WALLET', balance: 50 }),
    ]

    expect(computeAvailableBalance(accounts)).toBe(150)
  })
})

describe('computeTotalInvoice', () => {
  it('sums current invoices of active credit card accounts only', () => {
    const accounts = [
      makeAccount({ id: 1, type: 'CREDIT_CARD', balance: null, currentInvoice: 300 }),
      makeAccount({ id: 2, type: 'CREDIT_CARD', balance: null, currentInvoice: 150 }),
      makeAccount({ id: 3, type: 'CHECKING', balance: 1000 }),
    ]

    expect(computeTotalInvoice(accounts)).toBe(450)
  })

  it('excludes inactive credit card accounts', () => {
    const accounts = [
      makeAccount({ type: 'CREDIT_CARD', balance: null, currentInvoice: 300, active: false }),
    ]

    expect(computeTotalInvoice(accounts)).toBe(0)
  })

  it('returns zero when there are no credit card accounts', () => {
    const accounts = [makeAccount({ type: 'CHECKING', balance: 100 })]

    expect(computeTotalInvoice(accounts)).toBe(0)
  })
})

describe('computeProjectedBalance', () => {
  it('adds future income and subtracts future expense from the available balance', () => {
    expect(computeProjectedBalance(1000, 500, 200)).toBe(1300)
  })

  it('handles zero future income and expense', () => {
    expect(computeProjectedBalance(1000, 0, 0)).toBe(1000)
  })
})
