import { apiFetch } from '../../api/client'

/**
 * Dashboard payload shapes are not pinned (Appendix A #8/#9) — all fields
 * provisional and rendered defensively. Only the regions described in
 * overview.md §9 are rendered; nothing is invented (§7.10c).
 */

export interface DashboardCourseRef {
  id?: number | string
  title?: string
  code?: string
}

export interface DashboardQuizStatus {
  id?: number | string
  quizId?: number | string
  title?: string
  quizTitle?: string
  attempted?: boolean
  hasAttempt?: boolean
  bestScore?: number
  score?: number
}

export interface StudentDashboardPayload {
  enrolledCourses?: DashboardCourseRef[]
  courses?: DashboardCourseRef[]
  quizStatus?: DashboardQuizStatus[]
  quizzes?: DashboardQuizStatus[]
  /** possible per-course grouping: course → quiz statuses */
  courseQuizzes?: Array<DashboardCourseRef & { quizzes?: DashboardQuizStatus[]; quizStatus?: DashboardQuizStatus[] }>
  [key: string]: unknown
}

export interface InstructorDashboardCourse extends DashboardCourseRef {
  quizResults?: unknown[]
  quizResultSummaries?: unknown[]
}

export interface InstructorDashboardPayload {
  courses?: InstructorDashboardCourse[]
  announcements?: Array<{ id?: number | string; title?: string; createdAt?: string }>
  [key: string]: unknown
}

export interface AdminDashboardPayload {
  userCount?: number
  courseCount?: number
  enrollmentCount?: number
  users?: number
  courses?: number
  enrollments?: number
  [key: string]: unknown
}

export function getStudentDashboard(): Promise<StudentDashboardPayload> {
  return apiFetch<StudentDashboardPayload>('/api/students/me/dashboard')
}

export function getInstructorDashboard(): Promise<InstructorDashboardPayload> {
  return apiFetch<InstructorDashboardPayload>('/api/instructors/me/dashboard')
}

export function getAdminDashboard(): Promise<AdminDashboardPayload> {
  return apiFetch<AdminDashboardPayload>('/api/admin/dashboard')
}
