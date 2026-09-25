import { ApiError, responseToApiError } from './errors'
import { clearTokens, getAccessToken, getRefreshToken, notifySessionEnded, setAccessToken } from '../auth/token-store'

const BASE_URL: string = import.meta.env.VITE_API_BASE_URL ?? ''

let refreshInFlight: Promise<RefreshOutcome> | null = null

/**
 * `ok` — new access token stored.
 * `rejected` — the server refused the refresh token; the session is dead, clear it.
 * `unreachable` — no response at all (offline, proxy down, or the request was aborted by a
 * navigation mid-flight). Not an auth failure: the refresh token may still be perfectly
 * valid, so it must NOT be destroyed — the next page load retries it.
 */
type RefreshOutcome = 'ok' | 'rejected' | 'unreachable'

async function performRefresh(): Promise<RefreshOutcome> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return 'rejected'

  let response: Response
  try {
    response = await fetch(`${BASE_URL}/api/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    })
  } catch {
    return 'unreachable'
  }

  if (!response.ok) return 'rejected'

  try {
    const text = await response.text()
    if (!text) return 'rejected'
    const data = JSON.parse(text) as Record<string, unknown>
    const token = data.accessToken
    if (typeof token !== 'string' || !token) return 'rejected'

    setAccessToken(token)
    return 'ok'
  } catch {
    return 'rejected'
  }
}

function refreshSession(): Promise<RefreshOutcome> {
  if (!refreshInFlight) {
    refreshInFlight = performRefresh().finally(() => {
      refreshInFlight = null
    })
  }
  return refreshInFlight
}

/** Proactive refresh for session boot. Clears tokens only when the server rejected them. */
export async function tryRefreshSession(): Promise<boolean> {
  const outcome = await refreshSession()
  if (outcome === 'rejected') clearTokens()
  return outcome === 'ok'
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
    const outcome = await refreshSession()
    if (outcome === 'ok') {
      return apiFetch<T>(path, { ...options, skipAuthRetry: true })
    }
    if (outcome === 'rejected') {
      clearTokens()
      notifySessionEnded()
    }
    // `unreachable`: keep the session — a network blip isn't proof the tokens are dead.
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
