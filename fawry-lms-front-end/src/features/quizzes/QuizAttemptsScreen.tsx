import { Link, useParams, useSearchParams } from 'react-router'
import { isApiError } from '../../api/errors'
import { Card, PageHeader } from '../../components/Layout'
import { Button } from '../../components/Button'
import { EmptyState, ErrorState, ForbiddenState, Skeleton } from '../../components/States'
import { Pagination } from '../../components/Pagination'
import { formatDate } from '../../components/formatDate'
import { attemptStudentName } from './attempt-api'
import { useQuizAttempts } from './queries'

/** Staff-only list of all students' attempts — GET /api/quizzes/{id}/attempts (route matrix §6.1). */
export function QuizAttemptsScreen() {
  const { id: quizId = '' } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const page = Number(searchParams.get('page') ?? '0') || 0

  const attemptsQuery = useQuizAttempts(quizId, page)

  return (
    <div className="space-y-6">
      <PageHeader
        title="Quiz attempts"
        description="All student attempts for this quiz."
        actions={
          <Link to={`/quizzes/${quizId}`}>
            <Button variant="ghost">Back to quiz</Button>
          </Link>
        }
      />

      <Card className="!p-0">
        {attemptsQuery.isLoading ? (
          <div className="space-y-3 p-6">
            {Array.from({ length: 5 }, (_, i) => (
              <Skeleton key={i} className="h-10 w-full" />
            ))}
          </div>
        ) : attemptsQuery.isError ? (
          <div className="p-6">
            {isApiError(attemptsQuery.error) && attemptsQuery.error.status === 403 ? (
              <ForbiddenState message="You do not have access to attempts for this quiz." />
            ) : (
              <ErrorState onRetry={() => void attemptsQuery.refetch()} />
            )}
          </div>
        ) : !attemptsQuery.data || attemptsQuery.data.content.length === 0 ? (
          <div className="p-6">
            <EmptyState title="No attempts yet" description="Attempts appear after students submit." />
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50 text-left text-xs font-medium uppercase tracking-wide text-gray-500">
                <tr>
                  <th className="px-4 py-3">Student</th>
                  <th className="px-4 py-3">Started</th>
                  <th className="px-4 py-3">Submitted</th>
                  <th className="px-4 py-3">Score</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 bg-white">
                {attemptsQuery.data.content.map((row, index) => (
                  <tr key={String(row.id ?? index)}>
                    <td className="px-4 py-3 font-medium text-gray-900">{attemptStudentName(row)}</td>
                    <td className="px-4 py-3 text-gray-600">{formatDate(row.startedAt)}</td>
                    <td className="px-4 py-3 text-gray-600">
                      {row.submittedAt ? formatDate(row.submittedAt) : <span className="text-amber-700">In progress</span>}
                    </td>
                    <td className="px-4 py-3 font-medium text-gray-900">
                      {row.score !== undefined && row.score !== null
                        ? `${row.score}${row.totalQuestions !== undefined && row.totalQuestions !== null ? ` / ${row.totalQuestions}` : ''}`
                        : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="px-4 pb-4">
              <Pagination
                page={attemptsQuery.data.number}
                totalPages={attemptsQuery.data.totalPages}
                totalElements={attemptsQuery.data.totalElements}
                onPageChange={(next) => {
                  const params = new URLSearchParams(searchParams)
                  params.set('page', String(next))
                  setSearchParams(params)
                }}
              />
            </div>
          </div>
        )}
      </Card>
    </div>
  )
}
