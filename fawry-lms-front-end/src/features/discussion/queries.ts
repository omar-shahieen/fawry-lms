import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '../../api/keys'
import type { Page } from '../../api/page'
import { createPost, deleteDiscussion, listDiscussion, replyToPost, updateDiscussion } from './api'
import type { DiscussionFilters, DiscussionPost, DiscussionReply } from './api'

export function useDiscussion(courseId: string, filters: DiscussionFilters) {
  return useQuery({
    queryKey: queryKeys.discussion(courseId, filters as Record<string, unknown>),
    queryFn: () => listDiscussion(courseId, filters),
    enabled: Boolean(courseId),
  })
}

function useInvalidateDiscussion(courseId: string) {
  const queryClient = useQueryClient()
  return () => {
    void queryClient.invalidateQueries({ queryKey: ['course', courseId, 'discussion'] })
  }
}

export function useCreatePost(courseId: string) {
  const invalidate = useInvalidateDiscussion(courseId)
  return useMutation({
    mutationFn: (input: { title: string; body: string }) => createPost(courseId, input),
    onSuccess: invalidate,
  })
}

export function useReplyToPost(courseId: string) {
  const invalidate = useInvalidateDiscussion(courseId)
  return useMutation({
    mutationFn: ({ postId, body }: { postId: string | number; body: string }) => replyToPost(postId, body),
    onSuccess: invalidate,
  })
}

export function useUpdateDiscussion(courseId: string) {
  const invalidate = useInvalidateDiscussion(courseId)
  return useMutation({
    mutationFn: ({ id, input }: { id: string | number; input: { title?: string; body: string } }) =>
      updateDiscussion(id, input),
    onSuccess: invalidate,
  })
}

/** Optimistic removal with error rollback (§7.8.4). */
export function useDeleteDiscussion(courseId: string) {
  const queryClient = useQueryClient()
  const invalidate = useInvalidateDiscussion(courseId)
  return useMutation({
    mutationFn: (id: string | number) => deleteDiscussion(id),
    onMutate: async (id) => {
      const keyPrefix = ['course', courseId, 'discussion']
      await queryClient.cancelQueries({ queryKey: keyPrefix })
      const previous = queryClient.getQueriesData<Page<DiscussionPost>>({ queryKey: keyPrefix })
      for (const [key, data] of previous) {
        if (!data) continue
        queryClient.setQueryData(key, {
          ...data,
          content: data.content
            .filter((post) => String(post.id) !== String(id))
            .map((post) => ({
              ...post,
              replies: (post.replies ?? []).filter((reply) => String(reply.id) !== String(id)),
            })),
        })
      }
      return { previous }
    },
    onError: (_error, _id, context) => {
      for (const [key, data] of context?.previous ?? []) {
        queryClient.setQueryData(key, data)
      }
    },
    onSettled: invalidate,
  })
}

export type { DiscussionPost, DiscussionReply }
