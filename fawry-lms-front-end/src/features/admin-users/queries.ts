import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '../../api/keys'
import { createUser, deactivateUser, listUsers, updateUser } from './api'
import type { CreateUserInput, UpdateUserInput, UserListFilters } from './api'

export function useUsers(filters: UserListFilters) {
  return useQuery({
    queryKey: queryKeys.users(filters as Record<string, unknown>),
    queryFn: () => listUsers(filters),
  })
}

export function useCreateUser() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: CreateUserInput) => createUser(input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['users'] })
    },
  })
}

export function useUpdateUser() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, input }: { id: string | number; input: UpdateUserInput }) => updateUser(id, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['users'] })
    },
  })
}

export function useDeactivateUser() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string | number) => deactivateUser(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['users'] })
    },
  })
}
