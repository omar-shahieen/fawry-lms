import { apiFetch } from '../../api/client'

export interface QuizOption {
  id?: number | string
  text: string
  correct?: boolean
}

export interface QuizQuestion {
  id?: number | string
  text: string
  options: QuizOption[]
}

export interface Quiz {
  id: number | string
  title: string
  durationMinutes: number
  published: boolean
  courseId?: number | string
  questions?: QuizQuestion[]
}

export function listCourseQuizzes(courseId: string): Promise<Quiz[]> {
  return apiFetch<Quiz[]>(`/api/courses/${courseId}/quizzes`)
}

export function getQuiz(id: string): Promise<Quiz> {
  return apiFetch<Quiz>(`/api/quizzes/${id}`)
}

export interface CreateQuizInput {
  title: string
  durationMinutes: number
}

export function createQuiz(courseId: string, input: CreateQuizInput): Promise<Quiz> {
  return apiFetch<Quiz>(`/api/courses/${courseId}/quizzes`, { method: 'POST', body: input })
}

export function updateQuiz(
  id: string,
  input: Partial<CreateQuizInput> & { published?: boolean },
): Promise<Quiz> {
  return apiFetch<Quiz>(`/api/quizzes/${id}`, { method: 'PATCH', body: input })
}

export function deleteQuiz(id: string): Promise<void> {
  return apiFetch<void>(`/api/quizzes/${id}`, { method: 'DELETE' })
}

/**
 * Request DTO for add/update question is not pinned by either doc — this shape
 * (text + options[{text, correct}]) is provisional pending schema.d.ts.
 * Cardinality: one-or-more correct options (overview.md §4) — checkboxes, not radios.
 */
export interface QuestionInput {
  text: string
  options: Array<{ text: string; correct: boolean }>
}

export function createQuestion(quizId: string, input: QuestionInput): Promise<QuizQuestion> {
  return apiFetch<QuizQuestion>(`/api/quizzes/${quizId}/questions`, { method: 'POST', body: input })
}

export function updateQuestion(questionId: string, input: QuestionInput): Promise<QuizQuestion> {
  return apiFetch<QuizQuestion>(`/api/questions/${questionId}`, { method: 'PATCH', body: input })
}

export function deleteQuestion(questionId: string): Promise<void> {
  return apiFetch<void>(`/api/questions/${questionId}`, { method: 'DELETE' })
}
