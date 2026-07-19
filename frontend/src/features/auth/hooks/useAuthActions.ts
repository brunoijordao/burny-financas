import { useCallback } from 'react'
import { useNavigate } from 'react-router-dom'

import * as authApi from '@/features/auth/api/authApi'
import { useSessionStore } from '@/features/auth/store/sessionStore'
import type { LoginFormValues, RegisterFormValues } from '@/features/auth/schemas'

/**
 * Wires the auth screens to the backend endpoints and the in-memory session
 * store. Covers tasks 11.4 (login/register -> populate session) and 11.5
 * (logout -> revoke + clear session + redirect).
 */
export function useAuthActions() {
  const navigate = useNavigate()
  const setSession = useSessionStore((state) => state.setSession)
  const setEmail = useSessionStore((state) => state.setEmail)
  const clearSession = useSessionStore((state) => state.clearSession)
  const refreshToken = useSessionStore((state) => state.refreshToken)

  const loginWithCredentials = useCallback(
    async (values: LoginFormValues) => {
      const tokens = await authApi.login(values)
      setSession(tokens)
      setEmail(values.email)
      navigate('/', { replace: true })
    },
    [navigate, setSession, setEmail],
  )

  const registerAccount = useCallback(
    async (values: RegisterFormValues) => {
      await authApi.register({ email: values.email, password: values.password })
      // Registration does not log the user in automatically; send them to
      // login to authenticate with the credentials they just created.
      navigate('/login', { replace: true })
    },
    [navigate],
  )

  const logout = useCallback(async () => {
    try {
      if (refreshToken) {
        await authApi.logout(refreshToken)
      }
    } catch {
      // Logout is best-effort: even if the backend call fails (e.g. token
      // already revoked, network error) the client must still drop the
      // local session so the user is treated as logged out.
    } finally {
      clearSession()
      navigate('/login', { replace: true })
    }
  }, [clearSession, navigate, refreshToken])

  return { loginWithCredentials, registerAccount, logout }
}
