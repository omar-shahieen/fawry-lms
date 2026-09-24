import { Navigate, Route, Routes } from 'react-router'
import { useAuth } from '../auth/context'
import { FullPageSpinner, GuestOnly, RequireAuth, RequireRole } from '../auth/guards'
import { LoginScreen } from '../features/auth/LoginScreen'
import { SignupScreen } from '../features/auth/SignupScreen'
import { ProfileScreen } from '../features/profile/ProfileScreen'
import { AdminUsersScreen } from '../features/admin-users/AdminUsersScreen'
import { CatalogScreen } from '../features/courses/CatalogScreen'
import { CourseDetailScreen, CourseEditScreen } from '../features/courses/CourseScreens'
import { RosterScreen } from '../features/courses/RosterScreen'
import { AdminCoursesScreen } from '../features/courses/AdminCoursesScreen'
import { ContentReaderScreen } from '../features/sections/ContentReaderScreen'
import { ContentManageScreen } from '../features/sections/ContentManageScreen'
import { CourseQuizzesScreen, QuizManageScreen } from '../features/quizzes/QuizListScreens'
import { QuizEditScreen } from '../features/quizzes/QuizEditScreen'
import { QuizViewScreen } from '../features/quizzes/QuizViewScreen'
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
        <Route path="/courses" element={<CatalogScreen />} />
        <Route path="/courses/:id" element={<CourseDetailScreen />} />
        <Route path="/courses/:id/edit" element={<CourseEditScreen />} />
        <Route path="/courses/:id/students" element={<RosterScreen />} />
        <Route path="/courses/:id/content" element={<ContentReaderScreen />} />
        <Route path="/courses/:id/content/manage" element={<ContentManageScreen />} />
        <Route path="/courses/:id/quizzes" element={<CourseQuizzesScreen />} />
        <Route path="/courses/:id/quizzes/manage" element={<QuizManageScreen />} />
        <Route path="/quizzes/:id" element={<QuizViewScreen />} />
        <Route path="/quizzes/:id/edit" element={<QuizEditScreen />} />
        <Route
          path="/admin/courses"
          element={
            <RequireRole roles={['ADMIN']}>
              <AdminCoursesScreen />
            </RequireRole>
          }
        />
        <Route
          path="/admin/users"
          element={
            <RequireRole roles={['ADMIN']}>
              <AdminUsersScreen />
            </RequireRole>
          }
        />
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
