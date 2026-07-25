import { httpClient } from '@/lib/httpClient'

export type PlanningEntryType = 'PAYABLE' | 'RECEIVABLE'
export type PlanningEntryStatus = 'PENDING' | 'OVERDUE' | 'SETTLED'

export interface PlanningEntry {
  id: number
  type: PlanningEntryType
  accountId: number
  accountName: string
  categoryId: number | null
  categoryName: string | null
  amount: number
  description: string
  dueDate: string
  status: PlanningEntryStatus
  settledAt: string | null
  transactionId: number | null
  createdAt: string
  updatedAt: string
}

export interface CreatePlanningEntryPayload {
  type: PlanningEntryType
  accountId: number
  categoryId?: number
  amount: number
  description: string
  dueDate: string
}

export type UpdatePlanningEntryPayload = CreatePlanningEntryPayload

export interface ProjectedCashFlowPeriod {
  month: string
  totalReceivable: number
  totalPayable: number
  projectedBalance: number
}

export interface ProjectedCashFlow {
  currentAvailableBalance: number
  periods: ProjectedCashFlowPeriod[]
}

/** POST /planning-entries */
export async function createPlanningEntry(payload: CreatePlanningEntryPayload): Promise<PlanningEntry> {
  const response = await httpClient.post<PlanningEntry>('/planning-entries', payload)
  return response.data
}

/** GET /planning-entries -> the caller's active entries, ordered by due date */
export async function fetchPlanningEntries(): Promise<PlanningEntry[]> {
  const response = await httpClient.get<PlanningEntry[]>('/planning-entries')
  return response.data
}

/** PUT /planning-entries/{id} */
export async function updatePlanningEntry(id: number, payload: UpdatePlanningEntryPayload): Promise<PlanningEntry> {
  const response = await httpClient.put<PlanningEntry>(`/planning-entries/${id}`, payload)
  return response.data
}

/** DELETE /planning-entries/{id}, soft-deleted server-side */
export async function deletePlanningEntry(id: number): Promise<void> {
  await httpClient.delete(`/planning-entries/${id}`)
}

/** POST /planning-entries/{id}/settle -> creates a real transaction and links it back to the entry */
export async function settlePlanningEntry(id: number, settlementDate?: string): Promise<PlanningEntry> {
  const response = await httpClient.post<PlanningEntry>(`/planning-entries/${id}/settle`, null, {
    params: settlementDate ? { settlementDate } : undefined,
  })
  return response.data
}

/** POST /planning-entries/{id}/undo-settlement -> reverses the linked transaction's balance effect */
export async function undoPlanningEntrySettlement(id: number): Promise<PlanningEntry> {
  const response = await httpClient.post<PlanningEntry>(`/planning-entries/${id}/undo-settlement`)
  return response.data
}

/** GET /planning-entries/calendar?month=yyyy-MM */
export async function fetchPlanningCalendar(month: string): Promise<PlanningEntry[]> {
  const response = await httpClient.get<PlanningEntry[]>('/planning-entries/calendar', { params: { month } })
  return response.data
}

/** GET /planning-entries/projected-cash-flow?months=N */
export async function fetchProjectedCashFlow(months?: number): Promise<ProjectedCashFlow> {
  const response = await httpClient.get<ProjectedCashFlow>('/planning-entries/projected-cash-flow', {
    params: months ? { months } : undefined,
  })
  return response.data
}
