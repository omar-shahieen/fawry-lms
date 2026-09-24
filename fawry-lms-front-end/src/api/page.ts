/**
 * Spring Data Page envelope. Field names assumed from the Spring default and
 * pending confirmation against schema.d.ts (frontend-build.md Appendix A #1).
 */
export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
