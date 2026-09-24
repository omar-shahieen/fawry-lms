import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import { isApiError } from '../../api/errors'
import { queryKeys } from '../../api/keys'
import { useQuery } from '@tanstack/react-query'
import { Badge, Card, PageHeader } from '../../components/Layout'
import { Button } from '../../components/Button'
import { Select } from '../../components/FormField'
import { ConfirmDialog, Modal } from '../../components/Modal'
import { EmptyState, ErrorState, Skeleton } from '../../components/States'
import { Pagination } from '../../components/Pagination'
import { listUsers } from '../admin-users/api'
import { CourseFormFields } from './CourseScreens'
import { useAssignInstructor, useCourses, useCreateCourse, useDeleteCourse } from './queries'
import type { Course } from './api'

interface CourseFormValues {
  title: string
  description: string
  code: string
  term: string
}

const EMPTY_COURSE: CourseFormValues = { title: '', description: '', code: '', term: '' }

export function AdminCoursesScreen() {
  const [searchParams, setSearchParams] = useSearchParams()
  const page = Number(searchParams.get('page') ?? '0') || 0

  const coursesQuery = useCourses({ page, size: 20 })
  const createCourse = useCreateCourse()
  const deleteCourse = useDeleteCourse()
  const assignInstructor = useAssignInstructor()

  const instructorsQuery = useQuery({
    queryKey: queryKeys.users({ role: 'INSTRUCTOR', page: 0, size: 100 }),
    queryFn: () => listUsers({ role: 'INSTRUCTOR', page: 0, size: 100 }),
  })

  const [createOpen, setCreateOpen] = useState(false)
  const [formValues, setFormValues] = useState<CourseFormValues>(EMPTY_COURSE)
  const [formErrors, setFormErrors] = useState<{ title?: string; code?: string; term?: string; form?: string }>({})
  const [deleting, setDeleting] = useState<Course | null>(null)
  const [assigning, setAssigning] = useState<Course | null>(null)
  const [assignInstructorId, setAssignInstructorId] = useState('')
  const [assignError, setAssignError] = useState<string | null>(null)

  const setPage = (next: number) => {
    const params = new URLSearchParams(searchParams)
    if (next > 0) params.set('page', String(next))
    else params.delete('page')
    setSearchParams(params)
  }

  const onCreate = async (event: FormEvent) => {
    event.preventDefault()
    const nextErrors: typeof formErrors = {}
    if (!formValues.title.trim()) nextErrors.title = 'Title is required.'
    if (!formValues.code.trim()) nextErrors.code = 'Code is required.'
    if (!formValues.term.trim()) nextErrors.term = 'Term is required.'
    setFormErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) return

    try {
      await createCourse.mutateAsync(formValues)
      setCreateOpen(false)
      setFormValues(EMPTY_COURSE)
      setFormErrors({})
    } catch (error) {
      if (isApiError(error) && error.status === 409) setFormErrors({ code: 'This course code already exists.' })
      else if (isApiError(error) && error.fieldErrors) setFormErrors(error.fieldErrors)
      else if (isApiError(error)) setFormErrors({ form: error.message })
      else setFormErrors({ form: 'Something went wrong.' })
    }
  }

  const onAssign = async () => {
    if (!assigning || !assignInstructorId) return
    setAssignError(null)
    try {
      await assignInstructor.mutateAsync({ id: String(assigning.id), instructorId: assignInstructorId })
      setAssigning(null)
      setAssignInstructorId('')
    } catch (error) {
      if (isApiError(error)) setAssignError(error.message)
      else setAssignError('Something went wrong.')
    }
  }

  const onDelete = async () => {
    if (!deleting) return
    try {
      await deleteCourse.mutateAsync(String(deleting.id))
      setDeleting(null)
    } catch {
      setDeleting(null)
    }
  }

  const data = coursesQuery.data
  const instructors = instructorsQuery.data?.content ?? []

  return (
    <div className="space-y-6">
      <PageHeader
        title="Manage courses"
        description="Create courses, assign instructors, soft-delete."
        actions={<Button onClick={() => setCreateOpen(true)}>New course</Button>}
      />

      <Card className="!p-0">
        {coursesQuery.isLoading ? (
          <div className="space-y-3 p-6">
            {Array.from({ length: 5 }, (_, i) => (
              <Skeleton key={i} className="h-10 w-full" />
            ))}
          </div>
        ) : coursesQuery.isError ? (
          <div className="p-6">
            <ErrorState onRetry={() => void coursesQuery.refetch()} />
          </div>
        ) : !data || data.content.length === 0 ? (
          <div className="p-6">
            <EmptyState title="No courses yet" description="Create your first course to get started." />
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50 text-left text-xs font-medium uppercase tracking-wide text-gray-500">
                <tr>
                  <th className="px-4 py-3">Title</th>
                  <th className="px-4 py-3">Code</th>
                  <th className="px-4 py-3">Term</th>
                  <th className="px-4 py-3">Instructor</th>
                  <th className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 bg-white">
                {data.content.map((course) => {
                  const instructorLabel =
                    course.instructorName ??
                    (typeof course.instructor?.fullName === 'string' ? course.instructor.fullName : null)
                  return (
                    <tr key={String(course.id)}>
                      <td className="px-4 py-3">
                        <Link
                          to={`/courses/${course.id}`}
                          className="font-medium text-gray-900 hover:text-indigo-700"
                        >
                          {course.title}
                        </Link>
                      </td>
                      <td className="px-4 py-3">
                        <Badge>{course.code}</Badge>
                      </td>
                      <td className="px-4 py-3 text-gray-600">{course.term}</td>
                      <td className="px-4 py-3 text-gray-600">{instructorLabel ?? '—'}</td>
                      <td className="px-4 py-3 text-right">
                        <div className="flex justify-end gap-2">
                          <Button
                            size="sm"
                            variant="secondary"
                            onClick={() => {
                              setAssigning(course)
                              setAssignError(null)
                              setAssignInstructorId(
                                String(course.instructorId ?? course.instructor?.id ?? ''),
                              )
                            }}
                          >
                            Assign
                          </Button>
                          <Link to={`/courses/${course.id}/edit`}>
                            <Button size="sm" variant="secondary">
                              Edit
                            </Button>
                          </Link>
                          <Button size="sm" variant="danger" onClick={() => setDeleting(course)}>
                            Delete
                          </Button>
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
            <div className="px-4 pb-4">
              <Pagination
                page={data.number}
                totalPages={data.totalPages}
                totalElements={data.totalElements}
                onPageChange={setPage}
              />
            </div>
          </div>
        )}
      </Card>

      <Modal open={createOpen} title="Create course" onClose={() => setCreateOpen(false)}>
        <form onSubmit={onCreate} className="space-y-4" noValidate>
          {formErrors.form && (
            <div className="rounded-md bg-red-50 px-3 py-2 text-sm font-medium text-red-700" role="alert">
              {formErrors.form}
            </div>
          )}
          <CourseFormFields values={formValues} errors={formErrors} onChange={setFormValues} />
          <div className="flex justify-end">
            <Button type="submit" loading={createCourse.isPending}>
              Create course
            </Button>
          </div>
        </form>
      </Modal>

      <Modal open={assigning !== null} title="Assign instructor" onClose={() => setAssigning(null)}>
        <div className="space-y-4">
          {assignError && (
            <div className="rounded-md bg-red-50 px-3 py-2 text-sm font-medium text-red-700" role="alert">
              {assignError}
            </div>
          )}
          {instructorsQuery.isLoading ? (
            <Skeleton className="h-10 w-full" />
          ) : instructors.length === 0 ? (
            <p className="text-sm text-gray-500">No instructor accounts exist yet. Create one from Users first.</p>
          ) : (
            <Select
              label="Instructor"
              name="instructorId"
              value={assignInstructorId}
              onChange={(e) => setAssignInstructorId(e.target.value)}
            >
              <option value="">Select an instructor…</option>
              {instructors.map((instructor) => (
                <option key={String(instructor.id)} value={String(instructor.id)}>
                  {instructor.fullName} ({instructor.email})
                </option>
              ))}
            </Select>
          )}
          <div className="flex justify-end">
            <Button onClick={onAssign} loading={assignInstructor.isPending} disabled={!assignInstructorId}>
              Assign
            </Button>
          </div>
        </div>
      </Modal>

      <ConfirmDialog
        open={deleting !== null}
        title="Delete course"
        message={`Delete “${deleting?.title ?? ''}”? It will be removed from the catalog.`}
        confirmLabel="Delete"
        destructive
        loading={deleteCourse.isPending}
        onConfirm={onDelete}
        onCancel={() => setDeleting(null)}
      />
    </div>
  )
}
