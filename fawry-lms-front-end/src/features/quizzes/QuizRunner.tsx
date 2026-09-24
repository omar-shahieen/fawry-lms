import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { isApiError } from '../../api/errors'
import { queryKeys } from '../../api/keys'
import { Card } from '../../components/Layout'
import { Button } from '../../components/Button'
import { Skeleton } from '../../components/States'
import type { Quiz } from './api'
import { submitQuiz } from './attempt-api'
import type { SubmitAnswer, SubmitResult } from './attempt-api'
import { formatCountdown } from './useCountdown'

/** Single selection per question — server scores one selectedOptionId per question. */
interface Selections {
  [questionId: string]: number | undefined
}

export function QuizRunner({
  quiz,
  quizId,
  secondsLeft,
  expired,
  onSubmitted,
  onNoAttempt,
  onViewReview,
}: {
  quiz: Quiz
  quizId: string
  secondsLeft: number
  expired: boolean
  onSubmitted: () => void
  onNoAttempt: () => void
  onViewReview: () => void
}) {
  const [selections, setSelections] = useState<Selections>({})
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [lateRejected, setLateRejected] = useState(false)
  const queryClient = useQueryClient()

  const questions = quiz.questions ?? []

  const select = (questionId: number, optionId: number) => {
    setSelections((prev) => ({ ...prev, [String(questionId)]: optionId }))
  }

  const onSubmit = async () => {
    if (submitting) return
    setSubmitting(true)
    setError(null)

    const answers: SubmitAnswer[] = questions
      .filter((q) => q.id !== undefined && q.id !== null)
      .map((q) => {
        const selectedOptionId = selections[String(q.id)]
        return selectedOptionId === undefined
          ? { questionId: q.id! }
          : { questionId: q.id!, selectedOptionId }
      })

    try {
      const result: SubmitResult = await submitQuiz(quizId, answers)
      sessionStorage.setItem(`lms.quizResult.${quizId}`, JSON.stringify(result))
      // post-submit grades/dashboard/attempts are stale — refresh them quietly
      void queryClient.invalidateQueries({ queryKey: queryKeys.myGrades })
      void queryClient.invalidateQueries({ queryKey: queryKeys.studentDashboard })
      void queryClient.invalidateQueries({ queryKey: queryKeys.quizAttempts(quizId, {}) })
      onSubmitted()
    } catch (e) {
      if (isApiError(e) && e.status === 409) {
        // already submitted — single attempt only → straight to review
        onSubmitted()
      } else if (isApiError(e) && e.status === 400) {
        const message = e.message.toLowerCase()
        if (message.includes('expired')) {
          // late-reject: server refused the submission; offer the review view
          setError('The time limit has passed — your submission was not accepted.')
          setLateRejected(true)
        } else if (message.includes('not started')) {
          setError('No attempt was found for this quiz. Returning to the start…')
          setTimeout(() => onNoAttempt(), 1200)
        } else {
          setError(e.message)
        }
      } else if (isApiError(e)) {
        setError(e.message)
      } else {
        setError('Something went wrong. Please try again.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  const selectedCount = Object.values(selections).filter((id) => id !== undefined).length

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div className="sticky top-0 z-10 -mx-6 flex items-center justify-between gap-4 border-b border-gray-200 bg-gray-50/95 px-6 py-3 backdrop-blur lg:-mx-8 lg:px-8">
        <div>
          <h1 className="text-lg font-semibold text-gray-900">{quiz.title}</h1>
          <p className="text-xs text-gray-500">{questions.length} questions · single attempt</p>
        </div>
        <div
          className={`rounded-lg px-3 py-1.5 font-mono text-sm font-semibold tabular-nums ${
            expired ? 'bg-red-100 text-red-700' : secondsLeft < 60 ? 'bg-amber-100 text-amber-800' : 'bg-white text-gray-800 ring-1 ring-gray-200'
          }`}
          role="timer"
          aria-live="polite"
        >
          {formatCountdown(secondsLeft)}
        </div>
      </div>

      {expired && !lateRejected && (
        <div className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-800" role="alert">
          Time is up — submit now.
        </div>
      )}
      {error && (
        <div className="flex items-center justify-between gap-3 rounded-md bg-red-50 px-4 py-3 text-sm font-medium text-red-700" role="alert">
          <span>{error}</span>
          {lateRejected && (
            <Button size="sm" variant="secondary" onClick={onViewReview}>
              View review
            </Button>
          )}
        </div>
      )}

      {questions.length === 0 ? (
        <Card>
          <Skeleton className="h-6 w-2/3" />
          <Skeleton className="mt-3 h-4 w-full" />
          <Skeleton className="mt-2 h-4 w-full" />
        </Card>
      ) : (
        <div className="space-y-4">
          {questions.map((question, index) => {
            const questionId = question.id
            const selected = questionId !== undefined && questionId !== null ? selections[String(questionId)] : undefined
            return (
              <Card key={String(questionId ?? index)}>
                <p className="text-sm font-semibold text-gray-900">
                  {index + 1}. {question.text}
                </p>
                <div className="mt-3 space-y-2">
                  {(question.options ?? []).map((option) => {
                    const optionId = option.id
                    const checked = optionId !== undefined && optionId !== null && selected === optionId
                    return (
                      <label
                        key={String(optionId ?? option.text)}
                        className={`flex cursor-pointer items-center gap-3 rounded-md border px-3 py-2 text-sm transition-colors ${
                          checked ? 'border-indigo-400 bg-indigo-50' : 'border-gray-200 bg-white hover:bg-gray-50'
                        } ${expired || lateRejected ? 'pointer-events-none opacity-60' : ''}`}
                      >
                        <input
                          type="radio"
                          name={`question-${String(questionId ?? index)}`}
                          className="size-4 border-gray-300 text-indigo-600 focus:ring-indigo-600"
                          checked={checked}
                          disabled={expired || submitting || lateRejected}
                          onChange={() => {
                            if (questionId !== undefined && questionId !== null && optionId !== undefined && optionId !== null) {
                              select(questionId, optionId)
                            }
                          }}
                        />
                        <span className="text-gray-800">{option.text}</span>
                      </label>
                    )
                  })}
                </div>
              </Card>
            )
          })}
        </div>
      )}

      <div className="flex items-center justify-between gap-4 pb-8">
        <p className="text-xs text-gray-500">
          {selectedCount} answer{selectedCount === 1 ? '' : 's'} selected
        </p>
        <Button onClick={onSubmit} loading={submitting} disabled={lateRejected}>
          {expired ? 'Submit now' : 'Submit quiz'}
        </Button>
      </div>
    </div>
  )
}
