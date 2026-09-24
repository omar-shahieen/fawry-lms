import { Navigate, Route, Routes } from 'react-router'
import { useAuth } from '../auth/context'
import { FullPageSpinner, GuestOnly, RequireAuth } from '../auth/guards'
import { LoginScreen } from '../features/auth/LoginScreen'
import { SignupScreen } from '../features/auth/SignupScreen'
import { ProfileScreen } from '../features/profile/ProfileScreen'
import { DashboardPlaceholder } from './DashboardPlaceholder'
import { NotFoundPage } from './pages'
import { RootLayout } from './RootLayout'

function HomeRedirect() {
  const { status } = useAuth()
  if (status === 'loading') return <FullPageSpinner />
  return <Navigate to={status === 'authenticated' ? '/dashboard' : '/login'} replace />
}

export function AppRoutes() {
  return (
    <Routes>
      <Route
        path="/login"
        element={
          <GuestOnly>
            <LoginScreen />
          </GuestOnly>
        }
      />
      <Route
        path="/signup"
        element={
          <GuestOnly>
            <SignupScreen />
          </GuestOnly>
        }
      />
      <Route path="/" element={<HomeRedirect />} />

      <Route
        element={
          <RequireAuth>
            <RootLayout />
          </RequireAuth>
        }
      >
        <Route path="/dashboard" element={<DashboardPlaceholder />} />
        <Route path="/profile" element={<ProfileScreen />} />
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
