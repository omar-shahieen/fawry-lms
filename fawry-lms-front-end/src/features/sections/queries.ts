import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '../../api/keys'
import { createSection, deleteSection, listSections, updateSection } from './api'
import { createContent, deleteContent, listContent, updateContent } from '../content/api'
import type { ContentItem } from '../content/api'

export function useSections(courseId: string) {
  return useQuery({
    queryKey: queryKeys.sections(courseId),
    queryFn: () => listSections(courseId),
    enabled: Boolean(courseId),
  })
}

export function useSectionContent(sectionId: string | null) {
  return useQuery({
    queryKey: queryKeys.contentList(String(sectionId ?? '')),
    queryFn: () => listContent(String(sectionId)),
    enabled: Boolean(sectionId),
  })
}

function useInvalidateSections(courseId: string) {
  const queryClient = useQueryClient()
  return () => {
    void queryClient.invalidateQueries({ queryKey: queryKeys.sections(courseId) })
    void queryClient.invalidateQueries({ queryKey: ['section'] })
  }
}

export function useCreateSection(courseId: string) {
  const invalidate = useInvalidateSections(courseId)
  return useMutation({
    mutationFn: ({ title, orderIndex }: { title: string; orderIndex: number }) =>
      createSection(courseId, title, orderIndex),
    onSuccess: invalidate,
  })
}

export function useUpdateSection(courseId: string) {
  const invalidate = useInvalidateSections(courseId)
  return useMutation({
    mutationFn: ({ id, title, orderIndex }: { id: string; title: string; orderIndex: number }) =>
      updateSection(id, { title, orderIndex }),
    onSuccess: invalidate,
  })
}

export function useDeleteSection(courseId: string) {
  const invalidate = useInvalidateSections(courseId)
  return useMutation({
    mutationFn: (id: string) => deleteSection(id),
    onSuccess: invalidate,
  })
}

export function useCreateContent(sectionId: string, courseId: string) {
  const invalidate = useInvalidateSections(courseId)
  return useMutation({
    mutationFn: (input: { title: string; body: string }) => createContent(sectionId, input),
    onSuccess: invalidate,
  })
}

export function useUpdateContent(courseId: string) {
  const invalidate = useInvalidateSections(courseId)
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: { title: string; body: string } }) =>
      updateContent(id, input),
    onSuccess: invalidate,
  })
}

export function useDeleteContent(courseId: string) {
  const invalidate = useInvalidateSections(courseId)
  return useMutation({
    mutationFn: (id: string) => deleteContent(id),
    onSuccess: invalidate,
  })
}

export type { ContentItem }
