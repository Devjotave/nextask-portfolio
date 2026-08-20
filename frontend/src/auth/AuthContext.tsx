import {
  createContext,
  useContext,
  useMemo,
  useState,
  type PropsWithChildren,
} from 'react'

import {
  requestLogin,
  requestRegistration,
  type LoginInput,
  type RegisterInput,
  type User,
} from '../api/auth'
import {
  clearSession,
  createSession,
  readSession,
  saveSession,
  type AuthSession,
} from './session'

interface AuthContextValue {
  user: User | null
  token: string | null
  isAuthenticated: boolean
  login: (input: LoginInput) => Promise<void>
  register: (input: RegisterInput) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: PropsWithChildren) {
  const [session, setSession] = useState<AuthSession | null>(() => readSession())

  async function login(input: LoginInput): Promise<void> {
    const nextSession = createSession(await requestLogin(input))
    saveSession(nextSession)
    setSession(nextSession)
  }

  async function register(input: RegisterInput): Promise<void> {
    const nextSession = createSession(await requestRegistration(input))
    saveSession(nextSession)
    setSession(nextSession)
  }

  function logout(): void {
    clearSession()
    setSession(null)
  }

  const value = useMemo<AuthContextValue>(
    () => ({
      user: session?.user ?? null,
      token: session?.accessToken ?? null,
      isAuthenticated: session !== null,
      login,
      register,
      logout,
    }),
    [session],
  )

  return <AuthContext value={value}>{children}</AuthContext>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider')
  }

  return context
}
