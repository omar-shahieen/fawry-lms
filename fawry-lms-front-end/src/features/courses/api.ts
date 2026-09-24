import { apiFetch } from '../../api/client'
import type { Page } from '../../api/page'

export interface Course {
  id: number | string
  title: string
  code: string
  term: string
  description?: string
  /** Ownership fields pending schema.d.ts confirmation (Appendix A #9/#16). */
  instructorId?: number | string | null
  instructorName?: string | null
  instructor?: { id?: number | string; fullName?: string } | null
}

export interface CourseListFilters {
  search?: string
  term?: string
  code?: string
  page?: number
  size?: number
}

export function listCourses(filters: CourseListFilters): Promise<Page<Course>> {
  const params = new URLSearchParams()
  params.set('page', String(filters.page ?? 0))
  params.set('size', String(filters.size ?? 20))
  if (filters.search) params.set('search', filters.search)
  if (filters.term) params.set('term', filters.term)
  if (filters.code) params.set('code', filters.code)
  return apiFetch<Page<Course>>(`/api/courses?${params.toString()}`)
}

export function getCourse(id: string): Promise<Course> {
  return apiFetch<Course>(`/api/courses/${id}`)
}

export interface CreateCourseInput {
  title: string
  description: string
  code: string
  term: string
}

export function createCourse(input: CreateCourseInput): Promise<Course> {
  return apiFetch<Course>('/api/courses', { method: 'POST', body: input })
}

export function updateCourse(id: string, input: Partial<CreateCourseInput>): Promise<Course> {
  return apiFetch<Course>(`/api/courses/${id}`, { method: 'PATCH', body: input })
}

export function deleteCourse(id: string): Promise<void> {
  return apiFetch<void>(`/api/courses/${id}`, { method: 'DELETE' })
}

export function assignInstructor(id: string, instructorId: number | string): Promise<Course> {
  return apiFetch<Course>(`/api/courses/${id}/assign-instructor`, { method: 'PATCH', body: { instructorId } })
}

export function enrollInCourse(id: string): Promise<void> {
  return apiFetch<void>(`/api/courses/${id}/enroll`, { method: 'POST' })
}

export interface CourseStudent {
  id: number | string
  fullName: string
  email: string
}

export function listCourseStudents(id: string, page = 0, size = 20): Promise<Page<CourseStudent>> {
  return apiFetch<Page<CourseStudent>>(`/api/courses/${id}/students?page=${page}&size=${size}`)
}
