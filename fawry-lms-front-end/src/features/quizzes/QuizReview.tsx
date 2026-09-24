import { Card, PageHeader } from '../../components/Layout'
import { Badge } from '../../components/Layout'
import { formatDate } from '../../components/formatDate'
import type { Quiz } from './api'
import type { MyAttempt } from './attempt-api'

function readStoredResult(quizId: string): Record<string, unknown> | null {
  try {
    const raw = sessionStorage.getItem(`lms.quizResult.${quizId}`)
    return raw ? (JSON.parse(raw) as Record<string, unknown>) : null
  } catch {
    return null
  }
}

/**
 * Permanent post-submission review. Correctness/score fields render only when
 * the payload actually contains them (never pre-submit — that state cannot reach here).
 */
export function QuizReview({ quiz, attempt }: { quiz: Quiz; attempt: MyAttempt | null }) {
  const questions = quiz.questions ?? []
  const stored = readStoredResult(String(quiz.id))

  const score =
    attempt?.score ??
    (typeof stored?.score === 'number' ? stored.score : undefined)
  const totalFromStored = typeof stored?.totalQuestions === 'number' ? stored.totalQuestions : undefined
  const total = totalFromStored ?? (questions.length > 0 ? questions.length : undefined)

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <PageHeader
        title="Review"
        description="Your permanent record for this quiz — single attempt."
        actions={
          score !== undefined ? (
            <span className="rounded-lg bg-indigo-50 px-3 py-1.5 text-sm font-semibold text-indigo-700">
              Score: {score}
              {total !== undefined ? ` / ${total}` : ''}
            </span>
          ) : undefined
        }
      />

      {attempt?.submittedAt && (
        <div className="rounded-md bg-green-50 px-4 py-3 text-sm font-medium text-green-800">
          Submitted {formatDate(attempt.submittedAt)}
          {attempt.score !== undefined && attempt.score !== null ? ` · score ${attempt.score}` : ''}.
        </div>
      )}

      <div className="space-y-4">
        {questions.map((question, index) => {
          const optionList = question.options ?? []
          const correctIds = new Set(
            optionList.filter((o) => o.correct).map((o) => String(o.id ?? o.text)),
          )
          const hasCorrectness = correctIds.size > 0

          return (
            <Card key={String(question.id ?? index)}>
              <div className="flex items-start justify-between gap-3">
                <p className="text-sm font-semibold text-gray-900">
                  {index + 1}. {question.text}
                </p>
                {hasCorrectness && <Badge color="green">answered</Badge>}
              </div>
              <ul className="mt-3 space-y-2">
                {optionList.map((option) => {
                  const optionId = String(option.id ?? option.text)
                  const isCorrect = correctIds.has(optionId)
                  return (
                    <li
                      key={optionId}
                      className={`flex items-center gap-2 rounded-md border px-3 py-2 text-sm ${
                        isCorrect
                          ? 'border-green-300 bg-green-50 text-green-900'
                          : 'border-gray-200 bg-white text-gray-700'
                      }`}
                    >
                      <span
                        className={`inline-block size-2 rounded-full ${isCorrect ? 'bg-green-500' : 'bg-gray-300'}`}
                        aria-hidden="true"
                      />
                      {option.text}
                      {isCorrect && <span className="text-xs font-medium text-green-700">correct answer</span>}
                    </li>
                  )
                })}
              </ul>
            </Card>
          )
        })}
      </div>
    </div>
  )
}
