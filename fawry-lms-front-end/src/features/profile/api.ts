import { apiFetch } from '../../api/client'
import type { User } from '../../auth/types'

export interface UpdateProfileInput {
  fullName: string
  profilePictureUrl: string
}

export function updateProfile(input: UpdateProfileInput): Promise<User> {
  return apiFetch<User>('/api/users/me', { method: 'PATCH', body: input })
}
