import { apiFetch } from '../../api/client'
import type { Page } from '../../api/page'

/**
 * Attempt state probe — side-effect-free per lms-endpoints.md.
 * Always call this BEFORE GET /api/quizzes/{id} on the student take screen.
 *
 * Shape: QuizAttemptResponse (schema.d.ts).
 * - 404 → no attempt yet
 * - submittedAt set → permanent review
 * - submittedAt null → unsubmitted attempt, resume
 */
export interface MyAttempt {
  id?: number
  quizId?: number
  studentId?: string
  studentName?: string
  studentEmail?: string
  startedAt?: string
  submittedAt?: string | null
  score?: number
  totalQuestions?: number
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

/** SubmitAnswerRequest (schema.d.ts): one optional selected option per question. */
export interface SubmitAnswer {
  questionId: number
  selectedOptionId?: number
}

/** SubmitQuizResponse (schema.d.ts). */
export interface SubmitResult {
  attemptId?: number
  score?: number
  totalQuestions?: number
  submittedAt?: string
  answers?: Array<{ questionId?: number; selectedOptionId?: number; isCorrect?: boolean }>
}

export function submitQuiz(quizId: string, answers: SubmitAnswer[]): Promise<SubmitResult> {
  return apiFetch<SubmitResult>(`/api/quizzes/${quizId}/submit`, {
    method: 'POST',
    body: { answers },
  })
}

/** Staff attempts list — QuizAttemptResponse (schema.d.ts), flat fields. */
export interface QuizAttemptRow {
  id?: number
  quizId?: number
  studentId?: string
  studentName?: string
  studentEmail?: string
  startedAt?: string
  submittedAt?: string | null
  score?: number
  totalQuestions?: number
}

export function listQuizAttempts(quizId: string, page = 0, size = 20): Promise<Page<QuizAttemptRow>> {
  return apiFetch<Page<QuizAttemptRow>>(`/api/quizzes/${quizId}/attempts?page=${page}&size=${size}`)
}

export function attemptStudentName(row: QuizAttemptRow): string {
  return row.studentName ?? 'Student'
}
