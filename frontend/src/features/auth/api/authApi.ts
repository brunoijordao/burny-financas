import { httpClient } from '@/lib/httpClient'

export interface AuthTokens {
  accessToken: string
  refreshToken: string
}

export interface LoginPayload {
  email: string
  password: string
}

export interface RegisterPayload {
  email: string
  password: string
}

export interface LoginHistoryEntry {
  id: string
  ipAddress: string
  success: boolean
  createdAt: string
}

/** POST /auth/login -> { accessToken, refreshToken } */
export async function login(payload: LoginPayload): Promise<AuthTokens> {
  const response = await httpClient.post<AuthTokens>('/auth/login', payload)
  return response.data
}

/** POST /auth/register -> 2xx on success, no body assumed. */
export async function register(payload: RegisterPayload): Promise<void> {
  await httpClient.post('/auth/register', payload)
}

/** POST /auth/logout, revokes the given refresh token. Idempotent per spec. */
export async function logout(refreshToken: string): Promise<void> {
  await httpClient.post('/auth/logout', { refreshToken })
}

/** GET /auth/login-history, authenticated, returns the caller's own history. */
export async function fetchLoginHistory(): Promise<LoginHistoryEntry[]> {
  const response = await httpClient.get<LoginHistoryEntry[]>('/auth/login-history')
  return response.data
}
