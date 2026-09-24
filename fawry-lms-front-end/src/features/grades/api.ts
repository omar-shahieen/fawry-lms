import { apiFetch } from '../../api/client'
import type { Page } from '../../api/page'

/**
 * Grouped-by-course shape for GET /api/students/me/grades is not pinned
 * (Appendix A). Fields are provisional/optional; rendering is defensive.
 */
export interface StudentGradeEntry {
  quizId?: number | string
  quizTitle?: string
  quiz?: { id?: number | string; title?: string }
  score?: number
  submittedAt?: string
}

export interface StudentGradesCourse {
  courseId?: number | string
  courseTitle?: string
  courseCode?: string
  course?: { id?: number | string; title?: string; code?: string }
  grades?: StudentGradeEntry[]
  results?: StudentGradeEntry[]
}

export function getMyGrades(): Promise<StudentGradesCourse[]> {
  return apiFetch<StudentGradesCourse[]>('/api/students/me/grades')
}

/** Flat rows — field names {student, quiz, score} pinned by lms-endpoints.md prose. */
export interface StaffGradeRow {
  student: { id?: number | string; fullName?: string } | string
  quiz: { id?: number | string; title?: string } | string
  score: number
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
  return typeof row.student === 'string' ? row.student : (row.student.fullName ?? String(row.student.id ?? ''))
}

export function quizName(row: StaffGradeRow): string {
  return typeof row.quiz === 'string' ? row.quiz : (row.quiz.title ?? String(row.quiz.id ?? ''))
}
