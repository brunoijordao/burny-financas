import { httpClient } from '@/lib/httpClient'
import type { TransactionType } from '@/features/transactions/api/transactionsApi'

export type PdfImportStatus = 'PROCESSING' | 'READY_FOR_REVIEW' | 'FAILED'
export type PdfImportItemStatus = 'PENDING' | 'CONFIRMED' | 'DISCARDED'

export interface PdfImport {
  id: number
  accountId: number
  originalFilename: string
  status: PdfImportStatus
  failureReason: string | null
  createdAt: string
  updatedAt: string
}

export interface PdfImportItem {
  id: number
  transactionDate: string
  description: string
  amount: number
  type: TransactionType
  categoryId: number | null
  status: PdfImportItemStatus
  transactionId: number | null
  createdAt: string
}

export interface PdfImportDetail {
  pdfImport: PdfImport
  items: PdfImportItem[]
}

export interface UpdatePdfImportItemPayload {
  transactionDate: string
  description: string
  amount: number
  type: TransactionType
  categoryId?: number
}

/** POST /pdf-imports (multipart); processing happens asynchronously, poll fetchPdfImportDetail for status */
export async function uploadPdfImport(accountId: number, file: File): Promise<PdfImport> {
  const formData = new FormData()
  formData.append('file', file)
  const response = await httpClient.post<PdfImport>('/pdf-imports', formData, {
    params: { accountId },
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return response.data
}

/** GET /pdf-imports -> the caller's PDF imports */
export async function fetchPdfImports(): Promise<PdfImport[]> {
  const response = await httpClient.get<PdfImport[]>('/pdf-imports')
  return response.data
}

/** GET /pdf-imports/{id} -> status plus its line items */
export async function fetchPdfImportDetail(id: number): Promise<PdfImportDetail> {
  const response = await httpClient.get<PdfImportDetail>(`/pdf-imports/${id}`)
  return response.data
}

/** POST /pdf-imports/{id}/retry, re-processes the already-uploaded file */
export async function retryPdfImport(id: number): Promise<PdfImport> {
  const response = await httpClient.post<PdfImport>(`/pdf-imports/${id}/retry`)
  return response.data
}

/** PUT /pdf-imports/{id}/items/{itemId}, only allowed while the item is pending */
export async function updatePdfImportItem(
  importId: number,
  itemId: number,
  payload: UpdatePdfImportItemPayload,
): Promise<PdfImportItem> {
  const response = await httpClient.put<PdfImportItem>(`/pdf-imports/${importId}/items/${itemId}`, payload)
  return response.data
}

/** DELETE /pdf-imports/{id}/items/{itemId}, discards a pending item so it is never confirmed */
export async function discardPdfImportItem(importId: number, itemId: number): Promise<void> {
  await httpClient.delete(`/pdf-imports/${importId}/items/${itemId}`)
}

/** POST /pdf-imports/{id}/items/{itemId}/confirm, creates a real transaction with the account balance effect */
export async function confirmPdfImportItem(importId: number, itemId: number): Promise<PdfImportItem> {
  const response = await httpClient.post<PdfImportItem>(`/pdf-imports/${importId}/items/${itemId}/confirm`)
  return response.data
}
