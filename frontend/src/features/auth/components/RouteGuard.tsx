import { Navigate, Outlet, useLocation } from 'react-router-dom'

import { useSessionStore } from '@/features/auth/store/sessionStore'

/**
 * Blocks navigation to protected routes when there is no access token in
 * memory, redirecting to /login. Implements the "Frontend Route Guard"
 * requirement in specs/route-protection/spec.md.
 */
export function RouteGuard() {
  const accessToken = useSessionStore((state) => state.accessToken)
  const location = useLocation()

  if (!accessToken) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  return <Outlet />
}
