import { useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { isApiError } from '../../api/errors'
import { useAuth } from '../../auth/context'
import { Badge, Card, PageHeader } from '../../components/Layout'
import { Button } from '../../components/Button'
import { Input } from '../../components/FormField'
import { EmptyState, ErrorState, Skeleton } from '../../components/States'
import { Pagination } from '../../components/Pagination'
import { useCourses } from './queries'

export function CatalogScreen() {
  const [searchParams, setSearchParams] = useSearchParams()
  const { user } = useAuth()

  const search = searchParams.get('search') ?? ''
  const term = searchParams.get('term') ?? ''
  const code = searchParams.get('code') ?? ''
  const page = Number(searchParams.get('page') ?? '0') || 0

  const [draftSearch, setDraftSearch] = useState(search)
  const [draftTerm, setDraftTerm] = useState(term)
  const [draftCode, setDraftCode] = useState(code)

  const filters = { search, term, code, page, size: 12 }
  const coursesQuery = useCourses(filters)

  const applyFilters = (next: { search?: string; term?: string; code?: string; page?: number }) => {
    const params = new URLSearchParams()
    const values = {
      search: 'search' in next ? next.search : search,
      term: 'term' in next ? next.term : term,
      code: 'code' in next ? next.code : code,
    }
    if (values.search) params.set('search', values.search)
    if (values.term) params.set('term', values.term)
    if (values.code) params.set('code', values.code)
    if (next.page !== undefined && next.page > 0) params.set('page', String(next.page))
    setSearchParams(params)
  }

  const data = coursesQuery.data

  return (
    <div className="space-y-6">
      <PageHeader
        title="Courses"
        description={user?.role === 'ADMIN' ? 'All active courses.' : 'Browse the course catalog.'}
        actions={user?.role === 'ADMIN' ? <Link to="/admin/courses"><Button>Manage courses</Button></Link> : undefined}
      />

      <Card className="!p-4">
        <form
          className="flex flex-wrap items-end gap-3"
          onSubmit={(e) => {
            e.preventDefault()
            applyFilters({ search: draftSearch.trim(), term: draftTerm.trim(), code: draftCode.trim() })
          }}
        >
          <div className="min-w-48 flex-1">
            <Input
              label="Search"
              name="search"
              placeholder="Search by title…"
              value={draftSearch}
              onChange={(e) => setDraftSearch(e.target.value)}
            />
          </div>
          <div className="w-36">
            <Input label="Term" name="term" placeholder="e.g. Fall 2026" value={draftTerm} onChange={(e) => setDraftTerm(e.target.value)} />
          </div>
          <div className="w-28">
            <Input label="Code" name="code" placeholder="CS101" value={draftCode} onChange={(e) => setDraftCode(e.target.value)} />
          </div>
          <div className="flex gap-2">
            <Button type="submit">Apply</Button>
            {(search || term || code) && (
              <Button
                variant="secondary"
                onClick={() => {
                  setDraftSearch('')
                  setDraftTerm('')
                  setDraftCode('')
                  applyFilters({ search: '', term: '', code: '' })
                }}
              >
                Clear
              </Button>
            )}
          </div>
        </form>
      </Card>

      {coursesQuery.isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }, (_, i) => (
            <Skeleton key={i} className="h-40 w-full" />
          ))}
        </div>
      ) : coursesQuery.isError ? (
        <ErrorState
          message={isApiError(coursesQuery.error) ? coursesQuery.error.message : 'Failed to load courses.'}
          onRetry={() => void coursesQuery.refetch()}
        />
      ) : !data || data.content.length === 0 ? (
        <EmptyState title="No courses found" description="Try adjusting the filters." />
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {data.content.map((course) => (
              <Link key={String(course.id)} to={`/courses/${course.id}`} className="group block">
                <Card className="h-full transition-shadow group-hover:shadow-md">
                  <div className="flex items-start justify-between gap-2">
                    <h2 className="text-base font-semibold text-gray-900 group-hover:text-indigo-700">
                      {course.title}
                    </h2>
                    <Badge>{course.code}</Badge>
                  </div>
                  <p className="mt-1 text-xs text-gray-500">{course.term}</p>
                  {course.description && (
                    <p className="mt-3 line-clamp-3 text-sm text-gray-600">{course.description}</p>
                  )}
                </Card>
              </Link>
            ))}
          </div>
          <Pagination
            page={data.number}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            onPageChange={(next) => applyFilters({ page: next })}
          />
        </>
      )}
    </div>
  )
}
