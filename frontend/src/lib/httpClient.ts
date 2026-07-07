import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'

import { getSession } from '@/features/auth/store/sessionStore'

export const API_BASE_URL =
  (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8080'

/** Shared Axios instance used by every feature to talk to the backend. */
export const httpClient = axios.create({
  baseURL: API_BASE_URL,
})

// --- Request interceptor: attach the in-memory access token -----------------

httpClient.interceptors.request.use((config) => {
  const { accessToken } = getSession()
  if (accessToken) {
    config.headers.set('Authorization', `Bearer ${accessToken}`)
  }
  return config
})

// --- Response interceptor: single shared refresh-on-401 flow ---------------

interface RetriableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean
}

/**
 * Redirect handler used when the session becomes invalid. Kept overridable so
 * tests (and potentially a future in-app router integration) can observe/stub
 * navigation without relying on `window.location` inside jsdom.
 */
let onSessionExpired: () => void = () => {
  if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
    window.location.assign('/login')
  }
}

export function setSessionExpiredHandler(handler: () => void) {
  onSessionExpired = handler
}

/**
 * Shared in-flight refresh promise. When multiple requests fail with 401 at
 * the same time, they all await this single promise instead of each firing
 * their own `POST /auth/refresh` call.
 */
let refreshPromise: Promise<string> | null = null

async function performRefresh(): Promise<string> {
  const { refreshToken } = getSession()
  if (!refreshToken) {
    throw new Error('No refresh token available to attempt a session refresh.')
  }

  // Plain axios call (not `httpClient`) so this request never re-enters the
  // response interceptor and never carries a stale Authorization header.
  const response = await axios.post<{ accessToken: string; refreshToken: string }>(
    `${API_BASE_URL}/auth/refresh`,
    { refreshToken },
  )

  getSession().setSession(response.data)
  return response.data.accessToken
}

httpClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetriableRequestConfig | undefined

    const status = error.response?.status
    const isRefreshCall = originalRequest?.url?.includes('/auth/refresh')

    if (status !== 401 || !originalRequest || originalRequest._retry || isRefreshCall) {
      if (status === 401 && isRefreshCall) {
        // The refresh call itself was rejected: session is unrecoverable.
        getSession().clearSession()
        onSessionExpired()
      }
      return Promise.reject(error)
    }

    originalRequest._retry = true

    if (!refreshPromise) {
      refreshPromise = performRefresh().finally(() => {
        refreshPromise = null
      })
    }

    try {
      const newAccessToken = await refreshPromise
      originalRequest.headers.set('Authorization', `Bearer ${newAccessToken}`)
      return httpClient(originalRequest)
    } catch (refreshError) {
      getSession().clearSession()
      onSessionExpired()
      return Promise.reject(refreshError)
    }
  },
)
