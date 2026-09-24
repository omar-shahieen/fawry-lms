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
 * Token key names are not pinned by either doc (Appendix A #2).
 * Read the common candidates; tighten once schema.d.ts confirms.
 */
function applyTokens(data: unknown): void {
  if (typeof data !== 'object' || data === null) {
    throw new Error('Unexpected auth response: not an object')
  }
  const record = data as Record<string, unknown>
  const access = record.accessToken ?? record.access_token ?? record.token
  const refresh = record.refreshToken ?? record.refresh_token
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
