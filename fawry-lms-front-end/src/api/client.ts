import { ApiError, responseToApiError } from './errors'
import { clearTokens, getAccessToken, getRefreshToken, notifySessionEnded, setAccessToken } from '../auth/token-store'

const BASE_URL: string = import.meta.env.VITE_API_BASE_URL

let refreshInFlight: Promise<boolean> | null = null

async function performRefresh(): Promise<boolean> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return false

  try {
    const response = await fetch(`${BASE_URL}/api/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    })
    if (!response.ok) return false

    const text = await response.text()
    if (!text) return false
    const data = JSON.parse(text) as Record<string, unknown>
    const token = data.accessToken
    if (typeof token !== 'string' || !token) return false

    setAccessToken(token)
    return true
  } catch {
    return false
  }
}

function refreshSession(): Promise<boolean> {
  if (!refreshInFlight) {
    refreshInFlight = performRefresh().finally(() => {
      refreshInFlight = null
    })
  }
  return refreshInFlight
}

/** Proactive refresh for session boot. Clears tokens on failure; does not notify (caller owns state). */
export async function tryRefreshSession(): Promise<boolean> {
  const refreshed = await refreshSession()
  if (!refreshed) clearTokens()
  return refreshed
}

export interface ApiRequestOptions extends Omit<RequestInit, 'body'> {
  body?: unknown
  /** Internal: prevents the refresh call itself from re-entering retry logic. */
  skipAuthRetry?: boolean
}

export async function apiFetch<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const { body, skipAuthRetry, headers, ...rest } = options

  const requestHeaders = new Headers(headers)
  if (body !== undefined && !(body instanceof FormData)) {
    requestHeaders.set('Content-Type', 'application/json')
  }
  const token = getAccessToken()
  if (token) requestHeaders.set('Authorization', `Bearer ${token}`)

  const response = await fetch(`${BASE_URL}${path}`, {
    ...rest,
    headers: requestHeaders,
    body: body === undefined ? undefined : body instanceof FormData ? body : JSON.stringify(body),
  })

  if (response.status === 401 && !skipAuthRetry) {
    const refreshed = await refreshSession()
    if (refreshed) {
      return apiFetch<T>(path, { ...options, skipAuthRetry: true })
    }
    clearTokens()
    notifySessionEnded()
    throw await responseToApiError(response)
  }

  if (!response.ok) {
    throw await responseToApiError(response)
  }

  if (response.status === 204) return undefined as T

  const text = await response.text()
  if (!text) return undefined as T
  return JSON.parse(text) as T
}

export { ApiError }
