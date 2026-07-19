import { httpClient } from '@/lib/httpClient'

export interface Goal {
  id: number
  name: string
  targetAmount: number
  deadline: string
  currentAmount: number
  percentComplete: number
  completed: boolean
  projectedCompletionDate: string | null
  onTrack: boolean | null
  createdAt: string
  updatedAt: string
}

export interface Contribution {
  id: number
  goalId: number
  amount: number
  contributionDate: string
  createdAt: string
}

export interface CreateGoalPayload {
  name: string
  targetAmount: number
  deadline: string
}

export type UpdateGoalPayload = CreateGoalPayload

/** POST /goals */
export async function createGoal(payload: CreateGoalPayload): Promise<Goal> {
  const response = await httpClient.post<Goal>('/goals', payload)
  return response.data
}

/** GET /goals -> the caller's active goals, including completed ones */
export async function fetchGoals(): Promise<Goal[]> {
  const response = await httpClient.get<Goal[]>('/goals')
  return response.data
}

/** GET /goals/{id} */
export async function fetchGoal(id: number): Promise<Goal> {
  const response = await httpClient.get<Goal>(`/goals/${id}`)
  return response.data
}

/** PUT /goals/{id} */
export async function updateGoal(id: number, payload: UpdateGoalPayload): Promise<Goal> {
  const response = await httpClient.put<Goal>(`/goals/${id}`, payload)
  return response.data
}

/** DELETE /goals/{id}, soft-deleted server-side */
export async function deleteGoal(id: number): Promise<void> {
  await httpClient.delete(`/goals/${id}`)
}

/** GET /goals/{id}/contributions */
export async function fetchContributions(goalId: number): Promise<Contribution[]> {
  const response = await httpClient.get<Contribution[]>(`/goals/${goalId}/contributions`)
  return response.data
}

/** POST /goals/{id}/contributions */
export async function addContribution(goalId: number, amount: number, contributionDate?: string): Promise<Contribution> {
  const response = await httpClient.post<Contribution>(`/goals/${goalId}/contributions`, {
    amount,
    contributionDate,
  })
  return response.data
}
