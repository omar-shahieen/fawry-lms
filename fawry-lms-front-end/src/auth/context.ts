import { createContext, useContext } from 'react'
import type { LoginInput, SignupInput } from './api'
import type { User } from './types'

export type AuthStatus = 'loading' | 'authenticated' | 'anonymous'

export interface AuthContextValue {
  user: User | null
  status: AuthStatus
  signup: (input: SignupInput) => Promise<void>
  login: (input: LoginInput) => Promise<void>
  logout: () => Promise<void>
  refreshUser: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used within AuthProvider')
  return context
}
