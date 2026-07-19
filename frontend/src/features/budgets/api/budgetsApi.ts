import { httpClient } from '@/lib/httpClient'

export interface Budget {
  id: number
  categoryId: number
  categoryName: string
  icon: string
  color: string
  limitAmount: number | null
  spentAmount: number
  budgetMonth: string
  createdAt: string
  updatedAt: string
}

/** GET /budgets -> the caller's budgets for the current month, each with computed spend */
export async function fetchBudgets(): Promise<Budget[]> {
  const response = await httpClient.get<Budget[]>('/budgets')
  return response.data
}

/** PUT /budgets/categories/{categoryId} -> upserts the current month's budget for that category */
export async function setBudget(categoryId: number, limitAmount: number): Promise<Budget> {
  const response = await httpClient.put<Budget>(`/budgets/categories/${categoryId}`, { limitAmount })
  return response.data
}

/** DELETE /budgets/{id}, soft-deleted server-side */
export async function deleteBudget(id: number): Promise<void> {
  await httpClient.delete(`/budgets/${id}`)
}
