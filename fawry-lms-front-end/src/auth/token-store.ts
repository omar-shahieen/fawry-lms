const REFRESH_STORAGE_KEY = 'lms.refreshToken'

let accessToken: string | null = null
let sessionListeners: Array<() => void> = []

export function getAccessToken(): string | null {
  return accessToken
}

export function setAccessToken(token: string): void {
  accessToken = token
}

export function getRefreshToken(): string | null {
  try {
    return sessionStorage.getItem(REFRESH_STORAGE_KEY)
  } catch {
    return null
  }
}

export function setRefreshToken(token: string): void {
  try {
    sessionStorage.setItem(REFRESH_STORAGE_KEY, token)
  } catch {
    // sessionStorage unavailable (private mode) — session just won't survive reload
  }
}

export function clearTokens(): void {
  accessToken = null
  try {
    sessionStorage.removeItem(REFRESH_STORAGE_KEY)
  } catch {
    // ignore
  }
}

export function hasSession(): boolean {
  return accessToken !== null || getRefreshToken() !== null
}

export function onSessionEnded(listener: () => void): () => void {
  sessionListeners.push(listener)
  return () => {
    sessionListeners = sessionListeners.filter((l) => l !== listener)
  }
}

export function notifySessionEnded(): void {
  for (const listener of sessionListeners) listener()
}
