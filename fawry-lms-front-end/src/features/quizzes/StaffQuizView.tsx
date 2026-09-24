import { Link, useParams } from 'react-router'
import { queryKeys } from '../../api/keys'
import { Badge, Card, PageHeader } from '../../components/Layout'
import { Button } from '../../components/Button'
import { ErrorState, ForbiddenState, Skeleton } from '../../components/States'
import { useQuery } from '@tanstack/react-query'
import { isApiError } from '../../api/errors'
import { getQuiz, getQuizQuestions } from './api'

/**
 * Staff preview of a quiz — intentionally a SEPARATE component from the student
 * take screen (frontend-build.md §7.6.2 rule 5). Staff GET has no attempt side
 * effect and the detail payload is student-shaped (no correctness); correctness
 * comes from GET /api/quizzes/{id}/questions and must never be shared with the
 * student component.
 */
export function StaffQuizView() {
  const { id: quizId = '' } = useParams()

  const quizQuery = useQuery({
    queryKey: queryKeys.quiz(quizId),
    queryFn: () => getQuiz(quizId),
    enabled: Boolean(quizId),
  })

  const questionsQuery = useQuery({
    queryKey: queryKeys.quizQuestions(quizId),
    queryFn: () => getQuizQuestions(quizId),
    enabled: Boolean(quizId),
    retry: false,
  })

  if (quizQuery.isLoading) {
    return (
      <div className="space-y-3">
        <Skeleton className="h-8 w-1/3" />
        <Skeleton className="h-40 w-full" />
      </div>
    )
  }

  if (quizQuery.isError) {
    const error = quizQuery.error
    if (isApiError(error) && error.status === 403) return <ForbiddenState message="You do not have access to this quiz." />
    if (isApiError(error) && error.status === 404) return <ErrorState message="Quiz not found." />
    return <ErrorState onRetry={() => void quizQuery.refetch()} />
  }

  const quiz = quizQuery.data
  if (!quiz) return <ErrorState message="Quiz not found." />

  const questions = questionsQuery.data ?? []

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <PageHeader
        title={quiz.title}
        description={`${quiz.durationMinutes} minutes · staff preview (no attempt is created)`}
        actions={
          <div className="flex items-center gap-2">
            <Link to={`/quizzes/${quizId}/attempts`}>
              <Button variant="secondary">Attempts</Button>
            </Link>
            {quiz.published ? <Badge color="green">Published</Badge> : <Badge color="amber">Draft</Badge>}
          </div>
        }
      />

      <div className="space-y-4">
        {questionsQuery.isLoading && <Skeleton className="h-24 w-full" />}
        {questionsQuery.isError && (
          <ErrorState
            message={isApiError(questionsQuery.error) ? questionsQuery.error.message : 'Failed to load questions.'}
            onRetry={() => void questionsQuery.refetch()}
          />
        )}
        {questions.map((question, index) => (
          <Card key={String(question.id ?? index)}>
            <p className="text-sm font-semibold text-gray-900">
              {index + 1}. {question.text}
            </p>
            <ul className="mt-3 space-y-2">
              {(question.options ?? []).map((option, optionIndex) => (
                <li
                  key={String(option.id ?? optionIndex)}
                  className={`flex items-center gap-2 rounded-md border px-3 py-2 text-sm ${
                    option.isCorrect ? 'border-green-300 bg-green-50 text-green-900' : 'border-gray-200 bg-white text-gray-700'
                  }`}
                >
                  <span
                    className={`inline-block size-2 rounded-full ${option.isCorrect ? 'bg-green-500' : 'bg-gray-300'}`}
                    aria-hidden="true"
                  />
                  {option.text}
                  {option.isCorrect && <span className="text-xs font-medium text-green-700">correct</span>}
                </li>
              ))}
            </ul>
          </Card>
        ))}
        {!questionsQuery.isLoading && !questionsQuery.isError && questions.length === 0 && (
          <p className="text-sm text-gray-500">No questions yet.</p>
        )}
      </div>
    </div>
  )
}
