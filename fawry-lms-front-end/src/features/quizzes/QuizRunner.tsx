import { useState } from 'react'
import { isApiError } from '../../api/errors'
import { Card } from '../../components/Layout'
import { Button } from '../../components/Button'
import { Skeleton } from '../../components/States'
import type { Quiz } from './api'
import { submitQuiz } from './attempt-api'
import type { SubmitAnswer, SubmitResult } from './attempt-api'
import { formatCountdown } from './useCountdown'

interface Selections {
  [questionId: string]: Set<string>
}

export function QuizRunner({
  quiz,
  quizId,
  secondsLeft,
  expired,
  onSubmitted,
  onNoAttempt,
}: {
  quiz: Quiz
  quizId: string
  secondsLeft: number
  expired: boolean
  onSubmitted: () => void
  onNoAttempt: () => void
}) {
  const [selections, setSelections] = useState<Selections>({})
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const questions = quiz.questions ?? []

  const toggle = (questionId: string, optionId: string) => {
    setSelections((prev) => {
      const current = prev[questionId] ?? new Set<string>()
      const next = new Set(current)
      if (next.has(optionId)) next.delete(optionId)
      else next.add(optionId)
      return { ...prev, [questionId]: next }
    })
  }

  const onSubmit = async () => {
    if (submitting) return
    setSubmitting(true)
    setError(null)

    const answers: SubmitAnswer[] = questions
      .filter((q) => q.id !== undefined && q.id !== null)
      .map((q) => ({
        questionId: q.id!,
        optionIds: Array.from(selections[String(q.id)] ?? []),
      }))

    try {
      const result: SubmitResult = await submitQuiz(quizId, answers)
      sessionStorage.setItem(`lms.quizResult.${quizId}`, JSON.stringify(result))
      onSubmitted()
    } catch (e) {
      if (isApiError(e) && e.status === 409) {
        // already submitted — single attempt only → straight to review
        onSubmitted()
      } else if (isApiError(e) && e.status === 400) {
        setError('No attempt was found for this quiz. Returning to the start…')
        setTimeout(() => onNoAttempt(), 1200)
      } else if (isApiError(e)) {
        setError(e.message)
      } else {
        setError('Something went wrong. Please try again.')
      }
    } finally {
      setSubmitting(false)
    }
  }

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

      {expired && (
        <div className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-800" role="alert">
          Time is up — submit now.
        </div>
      )}
      {error && (
        <div className="rounded-md bg-red-50 px-4 py-3 text-sm font-medium text-red-700" role="alert">
          {error}
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
            const questionId = String(question.id ?? index)
            const selected = selections[questionId] ?? new Set<string>()
            return (
              <Card key={questionId}>
                <p className="text-sm font-semibold text-gray-900">
                  {index + 1}. {question.text}
                </p>
                <div className="mt-3 space-y-2">
                  {(question.options ?? []).map((option) => {
                    const optionId = String(option.id ?? option.text)
                    const checked = selected.has(optionId)
                    return (
                      <label
                        key={optionId}
                        className={`flex cursor-pointer items-center gap-3 rounded-md border px-3 py-2 text-sm transition-colors ${
                          checked ? 'border-indigo-400 bg-indigo-50' : 'border-gray-200 bg-white hover:bg-gray-50'
                        } ${expired ? 'pointer-events-none opacity-60' : ''}`}
                      >
                        <input
                          type="checkbox"
                          className="size-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-600"
                          checked={checked}
                          disabled={expired || submitting}
                          onChange={() => toggle(questionId, optionId)}
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
          {Object.values(selections).reduce((n, s) => n + s.size, 0)} answer
          {Object.values(selections).reduce((n, s) => n + s.size, 0) === 1 ? '' : 's'} selected
        </p>
        <Button onClick={onSubmit} loading={submitting}>
          {expired ? 'Submit now' : 'Submit quiz'}
        </Button>
      </div>
    </div>
  )
}
