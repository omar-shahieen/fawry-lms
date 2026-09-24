import { apiFetch } from '../../api/client'
import type { Page } from '../../api/page'

/** Shape not pinned — fields provisional pending schema.d.ts. */
export interface Announcement {
  id: number | string
  title: string
  body: string
  authorId?: number | string
  authorName?: string
  createdAt?: string
}

export interface AnnouncementFilters {
  page?: number
  size?: number
}

export function listAnnouncements(courseId: string, filters: AnnouncementFilters = {}): Promise<Page<Announcement>> {
  const params = new URLSearchParams()
  params.set('page', String(filters.page ?? 0))
  params.set('size', String(filters.size ?? 20))
  return apiFetch<Page<Announcement>>(`/api/courses/${courseId}/announcements?${params.toString()}`)
}

export function createAnnouncement(
  courseId: string,
  input: { title: string; body: string },
): Promise<Announcement> {
  return apiFetch<Announcement>(`/api/courses/${courseId}/announcements`, { method: 'POST', body: input })
}

export function updateAnnouncement(
  id: string | number,
  input: { title?: string; body?: string },
): Promise<Announcement> {
  return apiFetch<Announcement>(`/api/announcements/${id}`, { method: 'PATCH', body: input })
}

export function deleteAnnouncement(id: string | number): Promise<void> {
  return apiFetch<void>(`/api/announcements/${id}`, { method: 'DELETE' })
}
