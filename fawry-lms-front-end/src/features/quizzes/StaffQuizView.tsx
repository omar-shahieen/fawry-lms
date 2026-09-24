import { useParams } from 'react-router'
import { queryKeys } from '../../api/keys'
import { Badge, Card, PageHeader } from '../../components/Layout'
import { ErrorState, ForbiddenState, Skeleton } from '../../components/States'
import { useQuery } from '@tanstack/react-query'
import { isApiError } from '../../api/errors'
import { getQuiz } from './api'

/**
 * Staff preview of a quiz — intentionally a SEPARATE component from the student
 * take screen (frontend-build.md §7.6.2 rule 5). Staff GET has no attempt side
 * effect; whether the payload includes correctness info is confirmed live — if
 * present it renders here and must never be shared with the student component.
 */
export function StaffQuizView() {
  const { id: quizId = '' } = useParams()

  const quizQuery = useQuery({
    queryKey: queryKeys.quiz(quizId),
    queryFn: () => getQuiz(quizId),
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

  const questions = quiz.questions ?? []

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <PageHeader
        title={quiz.title}
        description={`${quiz.durationMinutes} minutes · staff preview (no attempt is created)`}
        actions={quiz.published ? <Badge color="green">Published</Badge> : <Badge color="amber">Draft</Badge>}
      />

      <div className="space-y-4">
        {questions.map((question, index) => (
          <Card key={String(question.id ?? index)}>
            <p className="text-sm font-semibold text-gray-900">
              {index + 1}. {question.text}
            </p>
            <ul className="mt-3 space-y-2">
              {(question.options ?? []).map((option, optionIndex) => (
                <li
                  key={optionIndex}
                  className={`flex items-center gap-2 rounded-md border px-3 py-2 text-sm ${
                    option.correct ? 'border-green-300 bg-green-50 text-green-900' : 'border-gray-200 bg-white text-gray-700'
                  }`}
                >
                  <span
                    className={`inline-block size-2 rounded-full ${option.correct ? 'bg-green-500' : 'bg-gray-300'}`}
                    aria-hidden="true"
                  />
                  {option.text}
                  {option.correct && <span className="text-xs font-medium text-green-700">correct</span>}
                </li>
              ))}
            </ul>
          </Card>
        ))}
        {questions.length === 0 && <p className="text-sm text-gray-500">No questions yet.</p>}
      </div>
    </div>
  )
}
