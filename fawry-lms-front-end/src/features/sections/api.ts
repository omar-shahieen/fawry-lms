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

export function createSection(courseId: string, title: string): Promise<Section> {
  return apiFetch<Section>(`/api/courses/${courseId}/sections`, { method: 'POST', body: { title } })
}

export function updateSection(id: string, input: { title: string; orderIndex: number }): Promise<Section> {
  return apiFetch<Section>(`/api/sections/${id}`, { method: 'PATCH', body: input })
}

export function deleteSection(id: string): Promise<void> {
  return apiFetch<void>(`/api/sections/${id}`, { method: 'DELETE' })
}
