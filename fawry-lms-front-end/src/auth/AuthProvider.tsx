import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { tryRefreshSession } from '../api/client'
import { fetchMe, login as apiLogin, logout as apiLogout, signup as apiSignup } from './api'
import type { LoginInput, SignupInput } from './api'
import { AuthContext } from './context'
import type { AuthStatus } from './context'
import { getRefreshToken, onSessionEnded } from './token-store'
import type { User } from './types'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [status, setStatus] = useState<AuthStatus>('loading')

  useEffect(() => {
    let cancelled = false

    async function boot() {
      if (!getRefreshToken()) {
        if (!cancelled) setStatus('anonymous')
        return
      }
      const refreshed = await tryRefreshSession()
      if (!refreshed) {
        if (!cancelled) setStatus('anonymous')
        return
      }
      try {
        const me = await fetchMe()
        if (!cancelled) {
          setUser(me)
          setStatus('authenticated')
        }
      } catch {
        if (!cancelled) {
          setUser(null)
          setStatus('anonymous')
        }
      }
    }

    void boot()
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(
    () =>
      onSessionEnded(() => {
        setUser(null)
        setStatus('anonymous')
      }),
    [],
  )

  const signup = useCallback(async (input: SignupInput) => {
    await apiSignup(input)
    const me = await fetchMe()
    setUser(me)
    setStatus('authenticated')
  }, [])

  const login = useCallback(async (input: LoginInput) => {
    await apiLogin(input)
    const me = await fetchMe()
    setUser(me)
    setStatus('authenticated')
  }, [])

  const logout = useCallback(async () => {
    await apiLogout()
    setUser(null)
    setStatus('anonymous')
  }, [])

  const value = useMemo(
    () => ({ user, status, signup, login, logout }),
    [user, status, signup, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
