import axios from 'axios'
import MockAdapter from 'axios-mock-adapter'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { useSessionStore } from '@/features/auth/store/sessionStore'
import { API_BASE_URL, httpClient, setSessionExpiredHandler } from './httpClient'

const REFRESH_URL = `${API_BASE_URL}/auth/refresh`

// `performRefresh` inside httpClient.ts intentionally calls the *global*
// axios instance (not `httpClient`) so the refresh call never re-enters the
// response interceptor. We mock both instances independently.
const mockHttp = new MockAdapter(httpClient)
const mockGlobalAxios = new MockAdapter(axios)

describe('httpClient auth interceptors', () => {
  beforeEach(() => {
    useSessionStore.getState().setSession({ accessToken: 'expired-token', refreshToken: 'valid-refresh' })
  })

  afterEach(() => {
    mockHttp.reset()
    mockGlobalAxios.reset()
    useSessionStore.getState().clearSession()
    setSessionExpiredHandler(() => {})
    vi.restoreAllMocks()
  })

  it('injects the in-memory access token on outgoing requests', async () => {
    mockHttp.onGet('/ping').reply((config) => {
      expect(config.headers?.Authorization).toBe('Bearer expired-token')
      return [200, { ok: true }]
    })

    const response = await httpClient.get('/ping')
    expect(response.data).toEqual({ ok: true })
  })

  it('retries a single request transparently after a 401 triggers a refresh', async () => {
    mockHttp
      .onGet('/protected')
      .replyOnce(401)
      .onGet('/protected')
      .reply((config) => {
        expect(config.headers?.Authorization).toBe('Bearer new-access-token')
        return [200, { secret: 42 }]
      })

    mockGlobalAxios.onPost(REFRESH_URL).reply(200, {
      accessToken: 'new-access-token',
      refreshToken: 'new-refresh-token',
    })

    const response = await httpClient.get('/protected')

    expect(response.data).toEqual({ secret: 42 })
    expect(mockGlobalAxios.history.post).toHaveLength(1)
    expect(useSessionStore.getState().accessToken).toBe('new-access-token')
    expect(useSessionStore.getState().refreshToken).toBe('new-refresh-token')
  })

  it('shares a single refresh call across concurrent 401s', async () => {
    mockHttp.onGet('/a').replyOnce(401).onGet('/a').reply(200, { from: 'a' })
    mockHttp.onGet('/b').replyOnce(401).onGet('/b').reply(200, { from: 'b' })

    mockGlobalAxios.onPost(REFRESH_URL).reply(200, {
      accessToken: 'shared-new-token',
      refreshToken: 'shared-new-refresh',
    })

    const [resA, resB] = await Promise.all([httpClient.get('/a'), httpClient.get('/b')])

    expect(resA.data).toEqual({ from: 'a' })
    expect(resB.data).toEqual({ from: 'b' })
    // Exactly one refresh call, no matter how many requests failed with 401 concurrently.
    expect(mockGlobalAxios.history.post).toHaveLength(1)
  })

  it('clears the session and redirects to login when the refresh itself fails', async () => {
    mockHttp.onGet('/protected').reply(401)
    mockGlobalAxios.onPost(REFRESH_URL).reply(401)

    const onExpired = vi.fn()
    setSessionExpiredHandler(onExpired)

    await expect(httpClient.get('/protected')).rejects.toBeTruthy()

    expect(useSessionStore.getState().accessToken).toBeNull()
    expect(useSessionStore.getState().refreshToken).toBeNull()
    expect(onExpired).toHaveBeenCalledTimes(1)
  })

  it('does not attempt a refresh when there is no refresh token available', async () => {
    useSessionStore.getState().clearSession()
    mockHttp.onGet('/protected').reply(401)

    const onExpired = vi.fn()
    setSessionExpiredHandler(onExpired)

    await expect(httpClient.get('/protected')).rejects.toBeTruthy()

    expect(mockGlobalAxios.history.post).toHaveLength(0)
    expect(onExpired).toHaveBeenCalledTimes(1)
  })
})
