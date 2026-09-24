import { useQuery } from '@tanstack/react-query'
import { queryKeys } from '../../api/keys'
import { getAdminDashboard, getInstructorDashboard, getStudentDashboard } from './api'

export function useStudentDashboard() {
  return useQuery({
    queryKey: queryKeys.studentDashboard,
    queryFn: getStudentDashboard,
  })
}

export function useInstructorDashboard() {
  return useQuery({
    queryKey: queryKeys.instructorDashboard,
    queryFn: getInstructorDashboard,
  })
}

export function useAdminDashboard() {
  return useQuery({
    queryKey: queryKeys.adminDashboard,
    queryFn: getAdminDashboard,
  })
}
