import { create } from 'zustand'

export interface SessionTokens {
  accessToken: string
  refreshToken: string
}

interface SessionState {
  accessToken: string | null
  refreshToken: string | null
  email: string | null
  /** Populates the in-memory session after a successful login/register/refresh. */
  setSession: (tokens: SessionTokens) => void
  /** Records the email used to log in, for display only (the backend does not return it). */
  setEmail: (email: string) => void
  /** Clears the in-memory session (logout, refresh failure). */
  clearSession: () => void
}

/**
 * Holds the access + refresh tokens ONLY in memory.
 *
 * Deliberately does NOT use Zustand's `persist` middleware (or any other
 * persistence) so that a full page reload always loses the session, per the
 * "Frontend Token Storage In Memory" requirement in
 * openspec/changes/setup-auth/specs/user-auth/spec.md. Never write these
 * tokens to localStorage/sessionStorage/cookies.
 */
export const useSessionStore = create<SessionState>((set) => ({
  accessToken: null,
  refreshToken: null,
  email: null,
  setSession: ({ accessToken, refreshToken }) => set({ accessToken, refreshToken }),
  setEmail: (email) => set({ email }),
  clearSession: () => set({ accessToken: null, refreshToken: null, email: null }),
}))

/** Non-hook accessor for use outside React components (e.g. Axios interceptors). */
export function getSession() {
  return useSessionStore.getState()
}
