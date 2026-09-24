import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '../../api/keys'
import { createAnnouncement, deleteAnnouncement, listAnnouncements, updateAnnouncement } from './api'
import type { AnnouncementFilters } from './api'

export function useAnnouncements(courseId: string, filters: AnnouncementFilters) {
  return useQuery({
    queryKey: queryKeys.announcements(courseId, filters as Record<string, unknown>),
    queryFn: () => listAnnouncements(courseId, filters),
    enabled: Boolean(courseId),
  })
}

function useInvalidateAnnouncements(courseId: string) {
  const queryClient = useQueryClient()
  return () => {
    void queryClient.invalidateQueries({ queryKey: ['course', courseId, 'announcements'] })
  }
}

export function useCreateAnnouncement(courseId: string) {
  const invalidate = useInvalidateAnnouncements(courseId)
  return useMutation({
    mutationFn: (input: { title: string; body: string }) => createAnnouncement(courseId, input),
    onSuccess: invalidate,
  })
}

export function useUpdateAnnouncement(courseId: string) {
  const invalidate = useInvalidateAnnouncements(courseId)
  return useMutation({
    mutationFn: ({ id, input }: { id: string | number; input: { title: string; body: string } }) =>
      updateAnnouncement(id, input),
    onSuccess: invalidate,
  })
}

export function useDeleteAnnouncement(courseId: string) {
  const invalidate = useInvalidateAnnouncements(courseId)
  return useMutation({
    mutationFn: (id: string | number) => deleteAnnouncement(id),
    onSuccess: invalidate,
  })
}
