/**
 * Spring Data Page envelope — field names confirmed against schema.d.ts
 * (Page*Response) and live responses (frontend-build.md Appendix A #1).
 */
export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
