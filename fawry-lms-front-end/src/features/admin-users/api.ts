import { apiFetch } from '../../api/client'
import type { Role, User } from '../../auth/types'
import type { Page } from '../../api/page'

export interface UserListFilters {
  role?: Role | ''
  page?: number
  size?: number
}

export function listUsers(filters: UserListFilters): Promise<Page<User>> {
  const params = new URLSearchParams()
  params.set('page', String(filters.page ?? 0))
  params.set('size', String(filters.size ?? 20))
  if (filters.role) params.set('role', filters.role)
  return apiFetch<Page<User>>(`/api/users?${params.toString()}`)
}

export interface CreateUserInput {
  fullName: string
  email: string
  password: string
  role: Role
}

export function createUser(input: CreateUserInput): Promise<User> {
  return apiFetch<User>('/api/users', { method: 'POST', body: input })
}

export interface UpdateUserInput {
  fullName: string
  role: Role
  email?: string
}

export function updateUser(id: string | number, input: UpdateUserInput): Promise<User> {
  return apiFetch<User>(`/api/users/${id}`, { method: 'PATCH', body: input })
}

export function deactivateUser(id: string | number): Promise<void> {
  return apiFetch<void>(`/api/users/${id}/deactivate`, { method: 'PATCH' })
}
