export type Role = 'STUDENT' | 'INSTRUCTOR' | 'ADMIN'

export interface EnrolledCourseRef {
  id?: number | string
  title?: string
  code?: string
}

export interface User {
  id: number | string
  fullName: string
  email: string
  role: Role
  profilePictureUrl?: string
  isActive?: boolean
  /** Present only for STUDENT — exact field name pending schema.d.ts (Appendix A #9). */
  enrolledCourses?: EnrolledCourseRef[]
}
