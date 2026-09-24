import { apiFetch } from '../../api/client'

/** Shapes per schema.d.ts — StudentDashboardCourseResponse[], InstructorDashboardResponse, AdminDashboardResponse. */

export interface StudentDashboardQuiz {
  quizId?: number
  quizTitle?: string
  attempted?: boolean
  score?: number
}

export interface StudentDashboardCourse {
  courseId?: number
  courseName?: string
  quizzes?: StudentDashboardQuiz[]
}

export interface InstructorDashboardCourse {
  courseId?: number
  courseName?: string
  submittedAttemptCount?: number
  averageScore?: number
}

export interface InstructorAnnouncement {
  id?: number
  courseId?: number
  title?: string
  body?: string
  createdAt?: string
}

export interface InstructorDashboard {
  courses?: InstructorDashboardCourse[]
  announcements?: InstructorAnnouncement[]
}

export interface AdminDashboard {
  userCountsByRole?: Record<string, number>
  totalCourseCount?: number
  totalEnrollmentCount?: number
}

export function getStudentDashboard(): Promise<StudentDashboardCourse[]> {
  return apiFetch<StudentDashboardCourse[]>('/api/students/me/dashboard')
}

export function getInstructorDashboard(): Promise<InstructorDashboard> {
  return apiFetch<InstructorDashboard>('/api/instructors/me/dashboard')
}

export function getAdminDashboard(): Promise<AdminDashboard> {
  return apiFetch<AdminDashboard>('/api/admin/dashboard')
}
