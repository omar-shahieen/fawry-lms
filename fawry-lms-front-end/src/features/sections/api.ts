import { apiFetch } from '../../api/client'

export interface Section {
  id: number | string
  title: string
  orderIndex: number
  courseId?: number | string
}

export function listSections(courseId: string): Promise<Section[]> {
  return apiFetch<Section[]>(`/api/courses/${courseId}/sections`)
}

/** CreateSectionRequest (schema.d.ts) — orderIndex required on create. */
export function createSection(courseId: string, title: string, orderIndex: number): Promise<Section> {
  return apiFetch<Section>(`/api/courses/${courseId}/sections`, { method: 'POST', body: { title, orderIndex } })
}

export function updateSection(id: string, input: { title: string; orderIndex: number }): Promise<Section> {
  return apiFetch<Section>(`/api/sections/${id}`, { method: 'PATCH', body: input })
}

export function deleteSection(id: string): Promise<void> {
  return apiFetch<void>(`/api/sections/${id}`, { method: 'DELETE' })
}
