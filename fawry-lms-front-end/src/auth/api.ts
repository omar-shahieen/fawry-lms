import { apiFetch } from '../api/client'
import { clearTokens, setAccessToken, setRefreshToken } from './token-store'
import type { User } from './types'

export interface SignupInput {
  fullName: string
  email: string
  password: string
}

export interface LoginInput {
  email: string
  password: string
}

/**
 * AuthResponse / RefreshResponse (schema.d.ts): accessToken + refreshToken on
 * auth, accessToken only on refresh. Confirmed against live responses.
 */
function applyTokens(data: unknown): void {
  if (typeof data !== 'object' || data === null) {
    throw new Error('Unexpected auth response: not an object')
  }
  const record = data as Record<string, unknown>
  const access = record.accessToken
  const refresh = record.refreshToken
  if (typeof access !== 'string' || !access) {
    throw new Error('Unexpected auth response: missing access token')
  }
  if (typeof refresh !== 'string' || !refresh) {
    throw new Error('Unexpected auth response: missing refresh token')
  }
  setAccessToken(access)
  setRefreshToken(refresh)
}

export async function signup(input: SignupInput): Promise<void> {
  const data = await apiFetch<unknown>('/api/auth/signup', { method: 'POST', body: input })
  applyTokens(data)
}

export async function login(input: LoginInput): Promise<void> {
  const data = await apiFetch<unknown>('/api/auth/login', { method: 'POST', body: input })
  applyTokens(data)
}

export async function logout(): Promise<void> {
  try {
    await apiFetch<void>('/api/auth/logout', { method: 'POST' })
  } catch {
    // best-effort: local tokens are discarded regardless
  } finally {
    clearTokens()
  }
}

export function fetchMe(): Promise<User> {
  return apiFetch<User>('/api/users/me')
}
