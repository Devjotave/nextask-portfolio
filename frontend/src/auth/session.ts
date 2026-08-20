import type { AuthResponse, User } from '../api/auth'

export interface AuthSession {
  accessToken: string
  tokenType: string
  expiresAt: number
  user: User
}

const SESSION_KEY = 'nextask.session'

function isUser(value: unknown): value is User {
  if (!value || typeof value !== 'object') {
    return false
  }

  const user = value as Partial<User>
  return (
    typeof user.id === 'number' &&
    typeof user.name === 'string' &&
    typeof user.email === 'string'
  )
}

function isSession(value: unknown): value is AuthSession {
  if (!value || typeof value !== 'object') {
    return false
  }

  const session = value as Partial<AuthSession>
  return (
    typeof session.accessToken === 'string' &&
    session.accessToken.length > 0 &&
    typeof session.tokenType === 'string' &&
    typeof session.expiresAt === 'number' &&
    isUser(session.user)
  )
}

export function createSession(response: AuthResponse): AuthSession {
  return {
    accessToken: response.accessToken,
    tokenType: response.tokenType,
    expiresAt: Date.now() + response.expiresIn * 1_000,
    user: response.user,
  }
}

export function readSession(): AuthSession | null {
  const serializedSession = sessionStorage.getItem(SESSION_KEY)

  if (!serializedSession) {
    return null
  }

  try {
    const session: unknown = JSON.parse(serializedSession)

    if (!isSession(session) || session.expiresAt <= Date.now()) {
      clearSession()
      return null
    }

    return session
  } catch {
    clearSession()
    return null
  }
}

export function saveSession(session: AuthSession): void {
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(session))
}

export function clearSession(): void {
  sessionStorage.removeItem(SESSION_KEY)
}
