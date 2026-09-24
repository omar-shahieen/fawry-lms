import { apiFetch } from '../../api/client'

/**
 * Attempt state probe — side-effect-free per lms-endpoints.md.
 * Always call this BEFORE GET /api/quizzes/{id} on the student take screen.
 *
 * Exact response shape is not pinned (Appendix A); handled defensively:
 * - 404 or empty body → no attempt yet
 * - object with submittedAt set → permanent review
 * - object with submittedAt null → unsubmitted attempt, resume
 */
export interface MyAttempt {
  id?: number | string
  startedAt?: string
  submittedAt?: string | null
  score?: number
  /** possible quiz summary fields on the response — used for the landing card if present */
  quizTitle?: string
  durationMinutes?: number
}

export async function getMyAttempt(quizId: string): Promise<MyAttempt | null> {
  try {
    const data = await apiFetch<MyAttempt | null>(`/api/quizzes/${quizId}/attempts/me`)
    return data ?? null
  } catch (error) {
    if (error && typeof error === 'object' && 'status' in error && (error as { status: number }).status === 404) {
      return null
    }
    throw error
  }
}

/**
 * Submit body shape is NOT pinned by either doc (Appendix A #3).
 * This provisional shape — per-question selected option ids, arrays for
 * multi-correct — must be confirmed against schema.d.ts before hardening.
 */
export interface SubmitAnswer {
  questionId: number | string
  optionIds: Array<number | string>
}

export interface SubmitResult {
  score?: number
  totalQuestions?: number
  maxScore?: number
  [key: string]: unknown
}

export function submitQuiz(quizId: string, answers: SubmitAnswer[]): Promise<SubmitResult> {
  return apiFetch<SubmitResult>(`/api/quizzes/${quizId}/submit`, {
    method: 'POST',
    body: { answers },
  })
}
