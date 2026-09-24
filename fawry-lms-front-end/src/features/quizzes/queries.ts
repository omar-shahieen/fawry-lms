import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '../../api/keys'
import {
  createQuestion,
  createQuiz,
  deleteQuestion,
  deleteQuiz,
  getQuiz,
  listCourseQuizzes,
  updateQuestion,
  updateQuiz,
} from './api'
import { listQuizAttempts } from './attempt-api'
import type { CreateQuizInput, QuestionInput, Quiz } from './api'

export function useQuizAttempts(quizId: string, page: number) {
  return useQuery({
    queryKey: queryKeys.quizAttempts(quizId, { page }),
    queryFn: () => listQuizAttempts(quizId, page),
    enabled: Boolean(quizId),
  })
}

export function useCourseQuizzes(courseId: string) {
  return useQuery({
    queryKey: queryKeys.courseQuizzes(courseId),
    queryFn: () => listCourseQuizzes(courseId),
    enabled: Boolean(courseId),
  })
}

export function useQuiz(id: string) {
  return useQuery({
    queryKey: queryKeys.quiz(id),
    queryFn: () => getQuiz(id),
    enabled: Boolean(id),
  })
}

function invalidateQuizList(courseId: string) {
  return (queryClient: { invalidateQueries: (args: { queryKey: readonly unknown[] }) => unknown }) => {
    void queryClient.invalidateQueries({ queryKey: queryKeys.courseQuizzes(courseId) })
    void queryClient.invalidateQueries({ queryKey: ['quiz'] })
  }
}

export function useCreateQuiz(courseId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: CreateQuizInput) => createQuiz(courseId, input),
    onSuccess: () => invalidateQuizList(courseId)(queryClient),
  })
}

export function useUpdateQuiz(courseId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: Partial<CreateQuizInput> & { published?: boolean } }) =>
      updateQuiz(id, input),
    onSuccess: (_data, variables) => {
      invalidateQuizList(courseId)(queryClient)
      void queryClient.invalidateQueries({ queryKey: queryKeys.quiz(variables.id) })
    },
  })
}

export function useDeleteQuiz(courseId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => deleteQuiz(id),
    onSuccess: () => invalidateQuizList(courseId)(queryClient),
  })
}

export function useCreateQuestion(quizId: string, courseId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: QuestionInput) => createQuestion(quizId, input),
    onSuccess: () => {
      invalidateQuizList(courseId)(queryClient)
      void queryClient.invalidateQueries({ queryKey: queryKeys.quiz(quizId) })
    },
  })
}

export function useUpdateQuestion(quizId: string, courseId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ questionId, input }: { questionId: string; input: QuestionInput }) =>
      updateQuestion(questionId, input),
    onSuccess: () => {
      invalidateQuizList(courseId)(queryClient)
      void queryClient.invalidateQueries({ queryKey: queryKeys.quiz(quizId) })
    },
  })
}

export function useDeleteQuestion(quizId: string, courseId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (questionId: string) => deleteQuestion(questionId),
    onSuccess: () => {
      invalidateQuizList(courseId)(queryClient)
      void queryClient.invalidateQueries({ queryKey: queryKeys.quiz(quizId) })
    },
  })
}

export type { Quiz }
