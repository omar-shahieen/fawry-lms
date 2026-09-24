import { useQuery } from '@tanstack/react-query'
import { queryKeys } from '../../api/keys'
import { getCourseGrades, getMyGrades } from './api'

export function useMyGrades() {
  return useQuery({
    queryKey: queryKeys.myGrades,
    queryFn: getMyGrades,
  })
}

export function useCourseGrades(courseId: string, page: number) {
  return useQuery({
    queryKey: [...queryKeys.courseGrades(courseId), page],
    queryFn: () => getCourseGrades(courseId, { page }),
    enabled: Boolean(courseId),
  })
}
