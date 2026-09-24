import { useEffect, useState } from 'react'
import { useParams } from 'react-router'
import { queryKeys } from '../../api/keys'
import { Badge, Card, PageHeader } from '../../components/Layout'
import { Button } from '../../components/Button'
import { ErrorState, ForbiddenState } from '../../components/States'
import { Spinner } from '../../components/Spinner'
import { getQuiz } from './api'
import { getMyAttempt } from './attempt-api'
import type { MyAttempt } from './attempt-api'
import { useQuery } from '@tanstack/react-query'
import { isApiError } from '../../api/errors'
import { useCountdown } from './useCountdown'
import { QuizReview } from './QuizReview'
import { QuizRunner } from './QuizRunner'

type Stance = 'probing' | 'need-start' | 'taking' | 'review'

/**
 * Student take screen. Hard rules (frontend-build.md §7.6.2 / react-quiz-attempt):
 * - GET /api/quizzes/{id} fires only after Start click, or to resume/review an existing attempt.
 * - attempts/me probe first (side-effect-free).
 * - This query: staleTime 0, gcTime 0 — never cached, never prefetched.
 */
export function QuizTakeScreen() {
  const { id: quizId = '' } = useParams()

  const [stance, setStance] = useState<Stance>('probing')
  const [attempt, setAttempt] = useState<MyAttempt | null>(null)

  // Quiz summary for the landing card, passed from the course quiz list —
  // never obtained by calling the side-effecting GET before Start.
  const [landingMeta] = useState(() => {
    const state = window.history.state?.usr as { quizTitle?: string; durationMinutes?: number } | null
    return state ?? null
  })

  const attemptQuery = useQuery({
    queryKey: queryKeys.quizAttemptMine(quizId),
    queryFn: () => getMyAttempt(quizId),
    staleTime: 0,
    gcTime: 0,
    retry: false,
    enabled: stance === 'probing' && Boolean(quizId),
  })

  // The side-effecting student GET — enabled only in taking/review stances.
  // Key includes stance so entering review always fetches the post-submit
  // payload (the pre-submit payload must never be reused for review).
  const quizQuery = useQuery({
    queryKey: [...queryKeys.quiz(quizId), 'take', stance],
    queryFn: () => getQuiz(quizId),
    staleTime: 0,
    gcTime: 0,
    retry: false,
    enabled: (stance === 'taking' || stance === 'review') && Boolean(quizId),
  })

  if (stance === 'probing' && attemptQuery.isSuccess) {
    const found = attemptQuery.data
    if (found && found.submittedAt) {
      setAttempt(found)
      setStance('review')
    } else if (found) {
      setAttempt(found)
      setStance('taking')
    } else {
      setAttempt(null)
      setStance('need-start')
    }
  }

  const quiz = quizQuery.data ?? null

  // Server-clock deadline — expiresAt on the detail payload (schema.d.ts).
  const [deadline, setDeadline] = useState<number | null>(null)
  useEffect(() => {
    if (!quiz || stance !== 'taking') {
      setDeadline(null)
      return
    }
    if (typeof quiz.expiresAt === 'string' && quiz.expiresAt) {
      const t = new Date(quiz.expiresAt).getTime()
      setDeadline(Number.isNaN(t) ? null : t)
    } else {
      setDeadline(null)
    }
  }, [quiz, stance])

  const secondsLeft = useCountdown(deadline)
  const expired = deadline !== null && secondsLeft <= 0

  if (stance === 'probing') {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <Spinner label="Checking your attempt…" />
      </div>
    )
  }

  if (stance === 'need-start') {
    return (
      <LandingCard
        meta={landingMeta}
        onStart={() => {
          // Explicit Start — the only place a student's first GET may fire.
          setStance('taking')
        }}
      />
    )
  }

  if (quizQuery.isError) {
    const err = quizQuery.error
    if (isApiError(err) && err.status === 403) {
      return (
        <ForbiddenState
          message="You are not enrolled in this course."
          action={
            <button
              type="button"
              className="text-sm font-medium text-indigo-600"
              onClick={() => void quizQuery.refetch()}
            >
              Retry
            </button>
          }
        />
      )
    }
    return (
      <ErrorState
        message={isApiError(err) ? err.message : 'Failed to load the quiz.'}
        onRetry={() => void quizQuery.refetch()}
      />
    )
  }

  if (!quiz) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <Spinner label={stance === 'review' ? 'Loading your review…' : 'Loading quiz…'} />
      </div>
    )
  }

  if (stance === 'review') {
    return <QuizReview quiz={quiz} attempt={attempt} />
  }

  return (
    <QuizRunner
      quiz={quiz}
      quizId={quizId}
      secondsLeft={secondsLeft}
      expired={expired}
      onSubmitted={() => setStance('review')}
      onNoAttempt={() => setStance('probing')}
      onViewReview={() => setStance('review')}
    />
  )
}

function LandingCard({
  meta,
  onStart,
}: {
  meta: { quizTitle?: string; durationMinutes?: number } | null
  onStart: () => void
}) {
  const title = meta?.quizTitle ?? 'Ready to begin?'
  const duration = meta?.durationMinutes

  return (
    <div className="mx-auto max-w-lg space-y-6">
      <PageHeader title="Quiz" />
      <Card>
        <div className="space-y-4">
          <div>
            <h2 className="text-lg font-semibold text-gray-900">{title}</h2>
            <div className="mt-2 flex flex-wrap gap-2 text-xs text-gray-600">
              {typeof duration === 'number' && <Badge color="indigo">{duration} minutes</Badge>}
              <Badge color="amber">Single attempt</Badge>
            </div>
          </div>

          <div className="rounded-md border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-medium text-amber-900">
            Starting creates your single attempt and starts the timer.
          </div>

          <ul className="list-disc space-y-1 pl-5 text-sm text-gray-600">
            <li>The timer is enforced by the server — once started, you cannot restart or retake.</li>
            <li>When time runs out, submit immediately; late submissions are rejected.</li>
          </ul>

          <Button className="w-full justify-center" onClick={onStart}>
            Start quiz
          </Button>
        </div>
      </Card>
    </div>
  )
}
