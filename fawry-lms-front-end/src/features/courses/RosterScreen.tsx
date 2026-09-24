import { useParams, useSearchParams } from 'react-router'
import { isApiError } from '../../api/errors'
import { Badge, Card, PageHeader } from '../../components/Layout'
import { ErrorState, EmptyState, ForbiddenState, Skeleton } from '../../components/States'
import { Pagination } from '../../components/Pagination'
import { useCourseStudents } from './queries'

export function RosterScreen() {
  const { id = '' } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const page = Number(searchParams.get('page') ?? '0') || 0
  const setPage = (next: number) => {
    const params = new URLSearchParams(searchParams)
    params.set('page', String(next))
    setSearchParams(params)
  }
  const studentsQuery = useCourseStudents(id, page)

  return (
    <div className="space-y-6">
      <PageHeader title="Students" description="Enrolled students in this course." />

      <Card className="!p-0">
        {studentsQuery.isLoading ? (
          <div className="space-y-3 p-6">
            {Array.from({ length: 5 }, (_, i) => (
              <Skeleton key={i} className="h-10 w-full" />
            ))}
          </div>
        ) : studentsQuery.isError ? (
          <div className="p-6">
            {isApiError(studentsQuery.error) && studentsQuery.error.status === 403 ? (
              <ForbiddenState message="You do not have access to this roster." />
            ) : (
              <ErrorState onRetry={() => void studentsQuery.refetch()} />
            )}
          </div>
        ) : !studentsQuery.data || studentsQuery.data.content.length === 0 ? (
          <div className="p-6">
            <EmptyState title="No students enrolled yet" />
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50 text-left text-xs font-medium uppercase tracking-wide text-gray-500">
                <tr>
                  <th className="px-4 py-3">Name</th>
                  <th className="px-4 py-3">Email</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 bg-white">
                {studentsQuery.data.content.map((student) => (
                  <tr key={String(student.id)}>
                    <td className="px-4 py-3 font-medium text-gray-900">
                      {student.fullName} <Badge>{String(student.id)}</Badge>
                    </td>
                    <td className="px-4 py-3 text-gray-600">{student.email}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="px-4 pb-4">
              <Pagination
                page={studentsQuery.data.number}
                totalPages={studentsQuery.data.totalPages}
                totalElements={studentsQuery.data.totalElements}
                onPageChange={setPage}
              />
            </div>
          </div>
        )}
      </Card>
    </div>
  )
}
