import { httpClient } from '@/lib/httpClient'

export type TransactionType = 'INCOME' | 'EXPENSE'
export type RecurrenceFrequency = 'WEEKLY' | 'MONTHLY' | 'YEARLY'

export interface Transaction {
  id: number
  description: string
  amount: number
  type: TransactionType
  transactionDate: string
  accountId: number
  categoryId: number | null
  note: string | null
  recurrenceId: number | null
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface PagedResult<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface CreateTransactionPayload {
  description: string
  amount: number
  type: TransactionType
  transactionDate: string
  accountId: number
  categoryId?: number
  note?: string
  recurring?: boolean
  frequency?: RecurrenceFrequency
  startDate?: string
  endDate?: string
}

export interface UpdateTransactionPayload {
  description: string
  amount: number
  type: TransactionType
  transactionDate: string
  accountId: number
  categoryId?: number
  note?: string
}

export interface TransactionFilters {
  accountId?: number
  categoryId?: number
  type?: TransactionType
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}

export interface TransactionAttachment {
  id: number
  transactionId: number
  originalFilename: string
  contentType: string
  sizeBytes: number
  createdAt: string
}

/** POST /transactions */
export async function createTransaction(payload: CreateTransactionPayload): Promise<Transaction> {
  const response = await httpClient.post<Transaction>('/transactions', payload)
  return response.data
}

/** GET /transactions -> the caller's active transactions, filtered and paginated */
export async function fetchTransactions(filters: TransactionFilters = {}): Promise<PagedResult<Transaction>> {
  const response = await httpClient.get<PagedResult<Transaction>>('/transactions', { params: filters })
  return response.data
}

/** PUT /transactions/{id} */
export async function updateTransaction(id: number, payload: UpdateTransactionPayload): Promise<Transaction> {
  const response = await httpClient.put<Transaction>(`/transactions/${id}`, payload)
  return response.data
}

/** DELETE /transactions/{id}, soft-deleted, reverses the account balance effect */
export async function deleteTransaction(id: number): Promise<void> {
  await httpClient.delete(`/transactions/${id}`)
}

/** DELETE /transactions/recurrences/{id}, stops future occurrence generation only */
export async function cancelRecurrence(recurrenceId: number): Promise<void> {
  await httpClient.delete(`/transactions/recurrences/${recurrenceId}`)
}

/** GET /transactions/{id}/attachments */
export async function fetchAttachments(transactionId: number): Promise<TransactionAttachment[]> {
  const response = await httpClient.get<TransactionAttachment[]>(`/transactions/${transactionId}/attachments`)
  return response.data
}

/** POST /transactions/{id}/attachments (multipart) */
export async function uploadAttachment(transactionId: number, file: File): Promise<TransactionAttachment> {
  const formData = new FormData()
  formData.append('file', file)
  const response = await httpClient.post<TransactionAttachment>(
    `/transactions/${transactionId}/attachments`,
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  )
  return response.data
}

/** GET /transactions/{id}/attachments/{attachmentId} -> downloads as a Blob */
export async function downloadAttachment(transactionId: number, attachmentId: number): Promise<Blob> {
  const response = await httpClient.get(`/transactions/${transactionId}/attachments/${attachmentId}`, {
    responseType: 'blob',
  })
  return response.data
}

/** DELETE /transactions/{id}/attachments/{attachmentId} */
export async function deleteAttachment(transactionId: number, attachmentId: number): Promise<void> {
  await httpClient.delete(`/transactions/${transactionId}/attachments/${attachmentId}`)
}
