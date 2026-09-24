import { apiFetch } from '../../api/client'

export interface ContentItem {
  id: number | string
  title: string
  body: string
  sectionId?: number | string
}

export function listContent(sectionId: string): Promise<ContentItem[]> {
  return apiFetch<ContentItem[]>(`/api/sections/${sectionId}/content`)
}

export function getContent(id: string): Promise<ContentItem> {
  return apiFetch<ContentItem>(`/api/content/${id}`)
}

export function createContent(sectionId: string, input: { title: string; body: string }): Promise<ContentItem> {
  return apiFetch<ContentItem>(`/api/sections/${sectionId}/content`, { method: 'POST', body: input })
}

export function updateContent(id: string, input: { title: string; body: string }): Promise<ContentItem> {
  return apiFetch<ContentItem>(`/api/content/${id}`, { method: 'PATCH', body: input })
}

export function deleteContent(id: string): Promise<void> {
  return apiFetch<void>(`/api/content/${id}`, { method: 'DELETE' })
}
