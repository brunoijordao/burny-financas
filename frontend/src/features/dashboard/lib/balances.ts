import type { Account } from '@/features/accounts/api/accountsApi'

/**
 * Sum of active non-CREDIT_CARD account balances. Deliberately not `GET /accounts/balance`,
 * which nets credit card invoices into the same number — see design.md Decision 2.
 */
export function computeAvailableBalance(accounts: Account[]): number {
  return accounts
    .filter((account) => account.active && account.type !== 'CREDIT_CARD')
    .reduce((sum, account) => sum + (account.balance ?? 0), 0)
}

/** Sum of active CREDIT_CARD current invoices, shown separately from the available balance. */
export function computeTotalInvoice(accounts: Account[]): number {
  return accounts
    .filter((account) => account.active && account.type === 'CREDIT_CARD')
    .reduce((sum, account) => sum + (account.currentInvoice ?? 0), 0)
}

/** Month-end projection: current available balance plus what's already booked for the rest of the month. */
export function computeProjectedBalance(availableBalance: number, futureIncome: number, futureExpense: number): number {
  return availableBalance + futureIncome - futureExpense
}
