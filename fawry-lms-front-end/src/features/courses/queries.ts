import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '../../api/keys'
import {
  assignInstructor,
  createCourse,
  deleteCourse,
  enrollInCourse,
  getCourse,
  listCourses,
  listCourseStudents,
  updateCourse,
} from './api'
import type { CourseListFilters, CreateCourseInput } from './api'

export function useCourses(filters: CourseListFilters) {
  return useQuery({
    queryKey: queryKeys.courses(filters as Record<string, unknown>),
    queryFn: () => listCourses(filters),
  })
}

export function useCourse(id: string) {
  return useQuery({
    queryKey: queryKeys.course(id),
    queryFn: () => getCourse(id),
    enabled: Boolean(id),
  })
}

export function useCourseStudents(id: string, page: number) {
  return useQuery({
    queryKey: [...queryKeys.courseStudents(id), page],
    queryFn: () => listCourseStudents(id, page),
    enabled: Boolean(id),
  })
}

function useInvalidateCourses() {
  const queryClient = useQueryClient()
  return () => {
    void queryClient.invalidateQueries({ queryKey: ['courses'] })
    void queryClient.invalidateQueries({ queryKey: ['course'] })
  }
}

export function useCreateCourse() {
  const invalidate = useInvalidateCourses()
  return useMutation({
    mutationFn: (input: CreateCourseInput) => createCourse(input),
    onSuccess: invalidate,
  })
}

export function useUpdateCourse() {
  const invalidate = useInvalidateCourses()
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: Partial<CreateCourseInput> }) => updateCourse(id, input),
    onSuccess: invalidate,
  })
}

export function useDeleteCourse() {
  const invalidate = useInvalidateCourses()
  return useMutation({
    mutationFn: (id: string) => deleteCourse(id),
    onSuccess: invalidate,
  })
}

export function useAssignInstructor() {
  const invalidate = useInvalidateCourses()
  return useMutation({
    mutationFn: ({ id, instructorId }: { id: string; instructorId: number | string }) =>
      assignInstructor(id, instructorId),
    onSuccess: invalidate,
  })
}

export function useEnrollCourse() {
  const invalidate = useInvalidateCourses()
  return useMutation({
    mutationFn: (id: string) => enrollInCourse(id),
    onSuccess: invalidate,
  })
}
