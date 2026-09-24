import { apiFetch } from '../../api/client'
import type { Page } from '../../api/page'

/** CourseGradesResponse / QuizGradeResponse / CourseGradeResponse (schema.d.ts). */
export interface QuizGrade {
  quizId?: number
  quizTitle?: string
  score?: number
  totalQuestions?: number
  submittedAt?: string
}

export interface StudentGradesCourse {
  courseId?: number
  courseTitle?: string
  courseCode?: string
  quizzes?: QuizGrade[]
}

export function getMyGrades(): Promise<StudentGradesCourse[]> {
  return apiFetch<StudentGradesCourse[]>('/api/students/me/grades')
}

export interface StaffGradeRow {
  student?: { id?: string; fullName?: string; email?: string }
  quiz?: { id?: number; title?: string }
  score?: number
  totalQuestions?: number
  submittedAt?: string
}

export interface CourseGradesFilters {
  page?: number
  size?: number
}

export function getCourseGrades(courseId: string, filters: CourseGradesFilters = {}): Promise<Page<StaffGradeRow>> {
  const params = new URLSearchParams()
  params.set('page', String(filters.page ?? 0))
  params.set('size', String(filters.size ?? 20))
  return apiFetch<Page<StaffGradeRow>>(`/api/courses/${courseId}/grades?${params.toString()}`)
}

export function studentName(row: StaffGradeRow): string {
  return row.student?.fullName ?? row.student?.email ?? 'Student'
}

export function quizName(row: StaffGradeRow): string {
  return row.quiz?.title ?? 'Quiz'
}
