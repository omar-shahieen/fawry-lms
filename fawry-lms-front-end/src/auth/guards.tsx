import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import { useAuth } from './context'
import type { Role } from './types'
import { Spinner } from '../components/Spinner'
import { ForbiddenPage } from '../routes/pages'

export function FullPageSpinner() {
  return (
    <div className="flex min-h-svh items-center justify-center bg-gray-50">
      <Spinner label="Loading…" />
    </div>
  )
}

export function RequireAuth({ children }: { children: ReactNode }) {
  const { status } = useAuth()
  const location = useLocation()

  if (status === 'loading') return <FullPageSpinner />
  if (status === 'anonymous') {
    const next = encodeURIComponent(`${location.pathname}${location.search}`)
    return <Navigate to={`/login?next=${next}`} replace />
  }
  return <>{children}</>
}

export function RequireRole({ roles, children }: { roles: Role[]; children: ReactNode }) {
  const { status, user } = useAuth()
  const location = useLocation()

  if (status === 'loading') return <FullPageSpinner />
  if (status === 'anonymous') {
    const next = encodeURIComponent(`${location.pathname}${location.search}`)
    return <Navigate to={`/login?next=${next}`} replace />
  }
  if (!user || !roles.includes(user.role)) return <ForbiddenPage />
  return <>{children}</>
}

/** Redirects authenticated users away from guest-only pages (login/signup). */
export function GuestOnly({ children }: { children: ReactNode }) {
  const { status } = useAuth()
  if (status === 'loading') return <FullPageSpinner />
  if (status === 'authenticated') return <Navigate to="/dashboard" replace />
  return <>{children}</>
}
