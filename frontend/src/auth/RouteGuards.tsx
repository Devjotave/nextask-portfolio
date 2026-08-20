import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'

import { useAuth } from './AuthContext'

interface RouteGuardProps {
  children: ReactNode
}

export function ProtectedRoute({ children }: RouteGuardProps) {
  const { isAuthenticated } = useAuth()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  return children
}

export function PublicOnlyRoute({ children }: RouteGuardProps) {
  const { isAuthenticated } = useAuth()

  if (isAuthenticated) {
    return <Navigate to="/app" replace />
  }

  return children
}
