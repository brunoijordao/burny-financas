import { afterEach, describe, expect, it } from 'vitest'

import { useSessionStore } from './sessionStore'

describe('useSessionStore', () => {
  afterEach(() => {
    useSessionStore.getState().clearSession()
    window.localStorage.clear()
    window.sessionStorage.clear()
  })

  it('starts with no session', () => {
    const state = useSessionStore.getState()
    expect(state.accessToken).toBeNull()
    expect(state.refreshToken).toBeNull()
  })

  it('setSession populates both tokens in memory', () => {
    useSessionStore.getState().setSession({ accessToken: 'access-1', refreshToken: 'refresh-1' })

    const state = useSessionStore.getState()
    expect(state.accessToken).toBe('access-1')
    expect(state.refreshToken).toBe('refresh-1')
  })

  it('clearSession removes both tokens', () => {
    useSessionStore.getState().setSession({ accessToken: 'access-1', refreshToken: 'refresh-1' })
    useSessionStore.getState().clearSession()

    const state = useSessionStore.getState()
    expect(state.accessToken).toBeNull()
    expect(state.refreshToken).toBeNull()
  })

  it('setEmail records the email used to log in', () => {
    useSessionStore.getState().setEmail('jane@example.com')
    expect(useSessionStore.getState().email).toBe('jane@example.com')
  })

  it('clearSession also clears the email', () => {
    useSessionStore.getState().setEmail('jane@example.com')
    useSessionStore.getState().clearSession()
    expect(useSessionStore.getState().email).toBeNull()
  })

  it('never writes tokens to localStorage or sessionStorage', () => {
    useSessionStore.getState().setSession({ accessToken: 'access-1', refreshToken: 'refresh-1' })

    expect(window.localStorage.length).toBe(0)
    expect(window.sessionStorage.length).toBe(0)
  })
})
