import { apiFetch } from '../../api/client'
import type { Page } from '../../api/page'

/** Student-facing option (QuizStudentOptionResponse) — never carries correctness pre-submit. */
export interface QuizOption {
  id?: number
  text: string
}

export interface QuizQuestion {
  id?: number
  text: string
  orderIndex?: number
  options: QuizOption[]
}

/** QuizAnswerResponse — present on the detail payload after submit. */
export interface QuizAnswer {
  questionId?: number
  selectedOptionId?: number
  isCorrect?: boolean
}

/** QuizResponse list item + QuizDetailResponse detail fields (schema.d.ts). */
export interface Quiz {
  id: number
  courseId?: number
  title: string
  durationMinutes: number
  published: boolean
  createdAt?: string
  updatedAt?: string
  startedAt?: string
  expiresAt?: string
  submittedAt?: string
  score?: number
  totalQuestions?: number
  questions?: QuizQuestion[]
  answers?: QuizAnswer[]
}

/** QuestionOptionResponse — staff question options carry isCorrect (GET /questions only). */
export interface StaffQuizOption {
  id?: number
  text?: string
  isCorrect?: boolean
}

/** QuestionResponse — GET /api/quizzes/{id}/questions (staff). */
export interface StaffQuizQuestion {
  id?: number
  text?: string
  orderIndex?: number
  options?: StaffQuizOption[]
}

export async function listCourseQuizzes(courseId: string): Promise<Quiz[]> {
  const page = await apiFetch<Page<Quiz>>(`/api/courses/${courseId}/quizzes`)
  return page.content ?? []
}

export function getQuiz(id: string): Promise<Quiz> {
  return apiFetch<Quiz>(`/api/quizzes/${id}`)
}

/** Staff questions with correctness — separate from the student detail payload. */
export function getQuizQuestions(id: string): Promise<StaffQuizQuestion[]> {
  return apiFetch<StaffQuizQuestion[]>(`/api/quizzes/${id}/questions`)
}

export interface CreateQuizInput {
  title: string
  durationMinutes: number
  published: boolean
}

export function createQuiz(courseId: string, input: CreateQuizInput): Promise<Quiz> {
  return apiFetch<Quiz>(`/api/courses/${courseId}/quizzes`, { method: 'POST', body: input })
}

export function updateQuiz(
  id: string,
  input: Partial<CreateQuizInput>,
): Promise<Quiz> {
  return apiFetch<Quiz>(`/api/quizzes/${id}`, { method: 'PATCH', body: input })
}

export function deleteQuiz(id: string): Promise<void> {
  return apiFetch<void>(`/api/quizzes/${id}`, { method: 'DELETE' })
}

/**
 * CreateQuestionRequest/UpdateQuestionRequest (schema.d.ts): text + orderIndex +
 * options[{text, isCorrect}]. orderIndex is required on create — caller supplies
 * the append position. Backend enforces exactly one isCorrect per question.
 */
export interface QuestionInput {
  text: string
  orderIndex?: number
  options: Array<{ text: string; isCorrect: boolean }>
}

export function createQuestion(quizId: string, input: QuestionInput): Promise<StaffQuizQuestion> {
  return apiFetch<StaffQuizQuestion>(`/api/quizzes/${quizId}/questions`, { method: 'POST', body: input })
}

export function updateQuestion(questionId: string, input: QuestionInput): Promise<StaffQuizQuestion> {
  return apiFetch<StaffQuizQuestion>(`/api/questions/${questionId}`, { method: 'PATCH', body: input })
}

export function deleteQuestion(questionId: string): Promise<void> {
  return apiFetch<void>(`/api/questions/${questionId}`, { method: 'DELETE' })
}
