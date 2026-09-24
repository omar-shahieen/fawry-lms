import { Link, useParams, useSearchParams } from 'react-router'
import { isApiError } from '../../api/errors'
import { Badge, Card, PageHeader } from '../../components/Layout'
import { Button } from '../../components/Button'
import { EmptyState, ErrorState, ForbiddenState, Skeleton } from '../../components/States'
import { Pagination } from '../../components/Pagination'
import { formatDate } from '../../components/formatDate'
import { useCourseGrades, useMyGrades } from './queries'
import { quizName, studentName } from './api'

export function MyGradesScreen() {
  const gradesQuery = useMyGrades()

  if (gradesQuery.isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-1/4" />
        <Skeleton className="h-32 w-full" />
        <Skeleton className="h-32 w-full" />
      </div>
    )
  }

  if (gradesQuery.isError) {
    const error = gradesQuery.error
    if (isApiError(error) && error.status === 403) {
      return <ForbiddenState message="Grades are only available to students." action={<Link to="/dashboard" className="text-sm font-medium text-indigo-600">Dashboard</Link>} />
    }
    return <ErrorState onRetry={() => void gradesQuery.refetch()} />
  }

  const groups = gradesQuery.data ?? []

  if (groups.length === 0) {
    return (
      <div className="space-y-6">
        <PageHeader title="Grades" description="Your quiz results by course." />
        <EmptyState title="No grades yet" description="Grades appear after you submit a quiz." action={<Link to="/courses"><Button variant="secondary">Browse courses</Button></Link>} />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <PageHeader title="Grades" description="Your quiz results, grouped by course." />
      <div className="space-y-4">
        {groups.map((group, index) => {
          const title = group.courseTitle ?? 'Course'
          const code = group.courseCode
          const entries = group.quizzes ?? []
          return (
            <Card key={String(group.courseId ?? index)}>
              <div className="flex items-center gap-2">
                <h2 className="text-sm font-semibold text-gray-900">{title}</h2>
                {code && <Badge>{code}</Badge>}
              </div>
              {entries.length === 0 ? (
                <p className="mt-2 text-sm text-gray-500">No quiz results yet.</p>
              ) : (
                <table className="mt-3 min-w-full text-sm">
                  <thead>
                    <tr className="border-b border-gray-100 text-left text-xs font-medium uppercase tracking-wide text-gray-500">
                      <th className="py-2 pr-4">Quiz</th>
                      <th className="py-2 pr-4">Score</th>
                      <th className="py-2">Submitted</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-50">
                    {entries.map((entry, entryIndex) => (
                      <tr key={String(entry.quizId ?? entryIndex)}>
                        <td className="py-2 pr-4 text-gray-800">{entry.quizTitle ?? 'Quiz'}</td>
                        <td className="py-2 pr-4 font-medium text-gray-900">
                          {entry.score !== undefined && entry.score !== null
                            ? `${entry.score}${entry.totalQuestions !== undefined ? ` / ${entry.totalQuestions}` : ''}`
                            : '—'}
                        </td>
                        <td className="py-2 text-gray-500">{formatDate(entry.submittedAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </Card>
          )
        })}
      </div>
    </div>
  )
}

export function CourseGradesScreen() {
  const { id: courseId = '' } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const page = Number(searchParams.get('page') ?? '0') || 0
  const setPage = (next: number) => {
    const params = new URLSearchParams(searchParams)
    params.set('page', String(next))
    setSearchParams(params)
  }
  const gradesQuery = useCourseGrades(courseId, page)

  return (
    <div className="space-y-6">
      <PageHeader
        title="Course grades"
        description="Quiz results for every student in this course."
        actions={
          <Link to={`/courses/${courseId}`}>
            <Button variant="ghost">Back to course</Button>
          </Link>
        }
      />

      <Card className="!p-0">
        {gradesQuery.isLoading ? (
          <div className="space-y-3 p-6">
            {Array.from({ length: 5 }, (_, i) => (
              <Skeleton key={i} className="h-10 w-full" />
            ))}
          </div>
        ) : gradesQuery.isError ? (
          <div className="p-6">
            {isApiError(gradesQuery.error) && gradesQuery.error.status === 403 ? (
              <ForbiddenState message="You do not have access to grades for this course." />
            ) : (
              <ErrorState onRetry={() => void gradesQuery.refetch()} />
            )}
          </div>
        ) : !gradesQuery.data || gradesQuery.data.content.length === 0 ? (
          <div className="p-6">
            <EmptyState title="No grades yet" description="Results appear once students submit quizzes." />
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50 text-left text-xs font-medium uppercase tracking-wide text-gray-500">
                <tr>
                  <th className="px-4 py-3">Student</th>
                  <th className="px-4 py-3">Email</th>
                  <th className="px-4 py-3">Quiz</th>
                  <th className="px-4 py-3">Score</th>
                  <th className="px-4 py-3">Submitted</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 bg-white">
                {gradesQuery.data.content.map((row, index) => (
                  <tr key={index}>
                    <td className="px-4 py-3 font-medium text-gray-900">{studentName(row)}</td>
                    <td className="px-4 py-3 text-gray-600">{row.student?.email ?? '—'}</td>
                    <td className="px-4 py-3 text-gray-600">{quizName(row)}</td>
                    <td className="px-4 py-3 font-medium text-gray-900">
                      {row.score !== undefined && row.score !== null
                        ? `${row.score}${row.totalQuestions !== undefined && row.totalQuestions !== null ? ` / ${row.totalQuestions}` : ''}`
                        : '—'}
                    </td>
                    <td className="px-4 py-3 text-gray-600">{formatDate(row.submittedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="px-4 pb-4">
              <Pagination
                page={gradesQuery.data.number}
                totalPages={gradesQuery.data.totalPages}
                totalElements={gradesQuery.data.totalElements}
                onPageChange={setPage}
              />
            </div>
          </div>
        )}
      </Card>
    </div>
  )
}
