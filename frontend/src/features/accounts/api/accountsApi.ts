import { httpClient } from '@/lib/httpClient'

export type AccountType =
  | 'CHECKING'
  | 'SAVINGS'
  | 'PHYSICAL_WALLET'
  | 'BROKERAGE'
  | 'DIGITAL_WALLET'
  | 'CREDIT_CARD'

export interface Account {
  id: number
  name: string
  icon: string
  color: string
  type: AccountType
  active: boolean
  balance: number | null
  creditLimit: number | null
  currentInvoice: number | null
  createdAt: string
  updatedAt: string
}

export interface CreateAccountPayload {
  name: string
  icon: string
  color: string
  type: AccountType
  creditLimit?: number
}

export interface UpdateAccountPayload {
  name: string
  icon: string
  color: string
  creditLimit?: number
}

export interface TransferPayload {
  sourceAccountId: number
  destinationAccountId: number
  amount: number
}

export interface TransferResult {
  id: number
  sourceAccountId: number
  destinationAccountId: number
  amount: number
  createdAt: string
}

export interface ConsolidatedBalance {
  consolidatedBalance: number
}

/** POST /accounts */
export async function createAccount(payload: CreateAccountPayload): Promise<Account> {
  const response = await httpClient.post<Account>('/accounts', payload)
  return response.data
}

/** GET /accounts -> the caller's active accounts */
export async function fetchAccounts(): Promise<Account[]> {
  const response = await httpClient.get<Account[]>('/accounts')
  return response.data
}

/** GET /accounts/balance */
export async function fetchConsolidatedBalance(): Promise<ConsolidatedBalance> {
  const response = await httpClient.get<ConsolidatedBalance>('/accounts/balance')
  return response.data
}

/** PUT /accounts/{id} */
export async function updateAccount(id: number, payload: UpdateAccountPayload): Promise<Account> {
  const response = await httpClient.put<Account>(`/accounts/${id}`, payload)
  return response.data
}

/** DELETE /accounts/{id}, soft-deleted server-side instead if it has linked history */
export async function deleteAccount(id: number): Promise<void> {
  await httpClient.delete(`/accounts/${id}`)
}

/** POST /accounts/transfers */
export async function transferBetweenAccounts(payload: TransferPayload): Promise<TransferResult> {
  const response = await httpClient.post<TransferResult>('/accounts/transfers', payload)
  return response.data
}
