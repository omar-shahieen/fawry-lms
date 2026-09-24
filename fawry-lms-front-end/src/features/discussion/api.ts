import { apiFetch } from '../../api/client'
import type { Page } from '../../api/page'

/**
 * Discussion post/reply shapes are not pinned (Appendix A #11 — reply nesting).
 * Fields provisional; author resolved defensively (flat or nested).
 */
export interface DiscussionAuthor {
  id?: number | string
  fullName?: string
}

export interface DiscussionReply {
  id: number | string
  body: string
  title?: string
  authorId?: number | string
  authorName?: string
  author?: DiscussionAuthor
  createdAt?: string
}

export interface DiscussionPost {
  id: number | string
  body: string
  title?: string
  authorId?: number | string
  authorName?: string
  author?: DiscussionAuthor
  createdAt?: string
  replies?: DiscussionReply[]
}

export interface DiscussionFilters {
  page?: number
  size?: number
}

export function listDiscussion(courseId: string, filters: DiscussionFilters = {}): Promise<Page<DiscussionPost>> {
  const params = new URLSearchParams()
  params.set('page', String(filters.page ?? 0))
  params.set('size', String(filters.size ?? 20))
  return apiFetch<Page<DiscussionPost>>(`/api/courses/${courseId}/discussion?${params.toString()}`)
}

/** CreateDiscussionPostRequest (schema.d.ts) — title required on create. */
export function createPost(
  courseId: string,
  input: { title: string; body: string },
): Promise<DiscussionPost> {
  return apiFetch<DiscussionPost>(`/api/courses/${courseId}/discussion`, { method: 'POST', body: input })
}

export function replyToPost(postId: string | number, body: string): Promise<DiscussionReply> {
  return apiFetch<DiscussionReply>(`/api/discussion/${postId}/reply`, { method: 'POST', body: { body } })
}

export function updateDiscussion(
  id: string | number,
  input: { title?: string; body: string },
): Promise<DiscussionPost> {
  return apiFetch<DiscussionPost>(`/api/discussion/${id}`, { method: 'PATCH', body: input })
}

export function deleteDiscussion(id: string | number): Promise<void> {
  return apiFetch<void>(`/api/discussion/${id}`, { method: 'DELETE' })
}

export function postAuthorId(item: { authorId?: number | string; author?: DiscussionAuthor }): number | string | null {
  return item.authorId ?? item.author?.id ?? null
}

export function postAuthorName(item: { authorName?: string; author?: DiscussionAuthor }): string {
  return item.authorName ?? item.author?.fullName ?? 'Unknown'
}
