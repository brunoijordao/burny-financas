import { httpClient } from '@/lib/httpClient'
import type { Transaction, TransactionType } from '@/features/transactions/api/transactionsApi'

export type ChatRole = 'USER' | 'MODEL'

export interface ChatMessagePayload {
  role: ChatRole
  text: string
}

export interface TransactionDraft {
  accountId: number
  accountName: string
  type: TransactionType
  amount: number
  description: string
  categoryId: number | null
  categoryName: string | null
  date: string
}

export interface ChatResponsePayload {
  reply: string
  draft: TransactionDraft | null
}

export interface ConfirmTransactionDraftPayload {
  description: string
  amount: number
  type: TransactionType
  date: string
  accountId: number
  categoryId: number | null
}

/** POST /ai-agent/messages, history is resent by the caller with each call (no server-side session) */
export async function sendChatMessage(
  message: string,
  history: ChatMessagePayload[],
): Promise<ChatResponsePayload> {
  const response = await httpClient.post<ChatResponsePayload>('/ai-agent/messages', { message, history })
  return response.data
}

/** POST /ai-agent/transactions/confirm, creates a real transaction; never calls the AI model */
export async function confirmTransactionDraft(payload: ConfirmTransactionDraftPayload): Promise<Transaction> {
  const response = await httpClient.post<Transaction>('/ai-agent/transactions/confirm', payload)
  return response.data
}
