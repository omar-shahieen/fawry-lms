import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { isApiError } from '../../api/errors'
import { useAuth } from '../../auth/context'
import { Badge, Card, PageHeader } from '../../components/Layout'
import { Button } from '../../components/Button'
import { ConfirmDialog } from '../../components/Modal'
import { ErrorState, ForbiddenState, Skeleton } from '../../components/States'
import { useCourse, useDeleteCourse, useEnrollCourse, useUpdateCourse } from './queries'

export function CourseDetailScreen() {
  const { id = '' } = useParams()
  const { user, refreshUser } = useAuth()
  const navigate = useNavigate()

  const courseQuery = useCourse(id)
  const enrollCourse = useEnrollCourse()
  const deleteCourse = useDeleteCourse()

  const [enrollError, setEnrollError] = useState<string | null>(null)
  const [confirmDelete, setConfirmDelete] = useState(false)

  if (courseQuery.isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-1/2" />
        <Skeleton className="h-40 w-full" />
      </div>
    )
  }

  if (courseQuery.isError) {
    const error = courseQuery.error
    if (isApiError(error) && error.status === 404) {
      return <ErrorState message="Course not found." />
    }
    if (isApiError(error) && error.status === 403) {
      return (
        <ForbiddenState
          message="You do not have access to this course."
          action={<Link to="/courses" className="text-sm font-medium text-indigo-600">Back to catalog</Link>}
        />
      )
    }
    return <ErrorState onRetry={() => void courseQuery.refetch()} />
  }

  const course = courseQuery.data
  if (!course || !user) return <ErrorState message="Course not found." />

  const isInstructorOwner =
    user.role === 'INSTRUCTOR' &&
    ((course.instructorId !== undefined && course.instructorId !== null && String(course.instructorId) === String(user.id)) ||
      (course.instructor && String(course.instructor.id) === String(user.id)))
  const canManage = user.role === 'ADMIN' || isInstructorOwner

  const enrolled =
    user.role === 'STUDENT' &&
    (user.enrolledCourses ?? []).some((c) => String(c.id) === String(course.id))

  const onEnroll = async () => {
    setEnrollError(null)
    try {
      await enrollCourse.mutateAsync(id)
      await refreshUser()
    } catch (error) {
      if (isApiError(error) && error.status === 409) {
        setEnrollError('Already enrolled in this course.')
        await refreshUser()
      } else if (isApiError(error)) {
        setEnrollError(error.message)
      } else {
        setEnrollError('Something went wrong. Please try again.')
      }
    }
  }

  const onDelete = async () => {
    try {
      await deleteCourse.mutateAsync(id)
      navigate('/courses', { replace: true })
    } catch {
      setConfirmDelete(false)
    }
  }

  const instructorLabel =
    course.instructorName ??
    (typeof course.instructor?.fullName === 'string' ? course.instructor.fullName : null)

  return (
    <div className="space-y-6">
      <PageHeader
        title={course.title}
        description={course.term}
        actions={
          <div className="flex flex-wrap items-center gap-2">
            <Badge>{course.code}</Badge>
            {user.role === 'STUDENT' && !enrolled && (
              <Button onClick={onEnroll} loading={enrollCourse.isPending}>
                Enroll
              </Button>
            )}
            {canManage && (
              <Link to={`/courses/${id}/edit`}>
                <Button variant="secondary">Edit</Button>
              </Link>
            )}
            {canManage && (
              <Link to={`/courses/${id}/students`}>
                <Button variant="secondary">Students</Button>
              </Link>
            )}
            {(canManage || (user.role === 'STUDENT' && enrolled)) && (
              <>
                <Link to={`/courses/${id}/content`}>
                  <Button variant="secondary">Content</Button>
                </Link>
                <Link to={`/courses/${id}/quizzes`}>
                  <Button variant="secondary">Quizzes</Button>
                </Link>
              </>
            )}
            {canManage && (
              <Link to={`/courses/${id}/content/manage`}>
                <Button variant="secondary">Manage content</Button>
              </Link>
            )}
            {user.role === 'ADMIN' && (
              <Button variant="danger" onClick={() => setConfirmDelete(true)}>
                Delete
              </Button>
            )}
          </div>
        }
      />

      {enrollError && (
        <div className="rounded-md bg-amber-50 px-3 py-2 text-sm font-medium text-amber-800" role="alert">
          {enrollError}
        </div>
      )}

      <Card>
        <dl className="space-y-4">
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-gray-500">Instructor</dt>
            <dd className="mt-1 text-sm text-gray-900">{instructorLabel ?? 'Not assigned'}</dd>
          </div>
          {course.description && (
            <div>
              <dt className="text-xs font-medium uppercase tracking-wide text-gray-500">Description</dt>
              <dd className="mt-1 whitespace-pre-wrap text-sm text-gray-700">{course.description}</dd>
            </div>
          )}
          {user.role === 'STUDENT' && (
            <div>
              <dt className="text-xs font-medium uppercase tracking-wide text-gray-500">Enrollment</dt>
              <dd className="mt-1 text-sm text-gray-900">
                {enrolled ? 'You are enrolled in this course.' : 'You are not enrolled in this course.'}
              </dd>
            </div>
          )}
        </dl>
      </Card>

      <ConfirmDialog
        open={confirmDelete}
        title="Delete course"
        message={`Delete “${course.title}”? Students will no longer see it in the catalog.`}
        confirmLabel="Delete"
        destructive
        loading={deleteCourse.isPending}
        onConfirm={onDelete}
        onCancel={() => setConfirmDelete(false)}
      />
    </div>
  )
}

export function CourseEditScreen() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const courseQuery = useCourse(id)
  const updateCourse = useUpdateCourse()
  const [errors, setErrors] = useState<{ title?: string; code?: string; term?: string; form?: string }>({})
  const [values, setValues] = useState<{ title: string; description: string; code: string; term: string } | null>(null)

  const loaded = courseQuery.data
  const [initialized, setInitialized] = useState(false)
  if (loaded && !initialized && !values) {
    setValues({
      title: loaded.title,
      description: loaded.description ?? '',
      code: loaded.code,
      term: loaded.term,
    })
    setInitialized(true)
  }

  if (courseQuery.isLoading || !values) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-1/2" />
        <Skeleton className="h-64 w-full" />
      </div>
    )
  }

  if (courseQuery.isError) {
    return <ErrorState onRetry={() => void courseQuery.refetch()} />
  }

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault()
    const nextErrors: typeof errors = {}
    if (!values.title.trim()) nextErrors.title = 'Title is required.'
    if (!values.code.trim()) nextErrors.code = 'Code is required.'
    if (!values.term.trim()) nextErrors.term = 'Term is required.'
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) return

    try {
      await updateCourse.mutateAsync({ id, input: values })
      navigate(`/courses/${id}`, { replace: true })
    } catch (error) {
      if (isApiError(error) && error.status === 409) setErrors({ code: 'This course code already exists.' })
      else if (isApiError(error) && error.fieldErrors) setErrors(error.fieldErrors)
      else if (isApiError(error)) setErrors({ form: error.message })
      else setErrors({ form: 'Something went wrong.' })
    }
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <PageHeader title="Edit course" />
      <Card>
        <form onSubmit={onSubmit} className="space-y-4" noValidate>
          {errors.form && (
            <div className="rounded-md bg-red-50 px-3 py-2 text-sm font-medium text-red-700" role="alert">
              {errors.form}
            </div>
          )}
          <CourseFormFields values={values} errors={errors} onChange={setValues} />
          <div className="flex justify-end gap-3">
            <Button variant="secondary" onClick={() => navigate(`/courses/${id}`)}>
              Cancel
            </Button>
            <Button type="submit" loading={updateCourse.isPending}>
              Save changes
            </Button>
          </div>
        </form>
      </Card>
    </div>
  )
}

export function CourseFormFields({
  values,
  errors,
  onChange,
}: {
  values: { title: string; description: string; code: string; term: string }
  errors: { title?: string; code?: string; term?: string }
  onChange: (values: { title: string; description: string; code: string; term: string }) => void
}) {
  return (
    <>
      <label className="block space-y-1.5">
        <span className="block text-sm font-medium text-gray-800">Title</span>
        <input
          className="block w-full rounded-md border-0 bg-white px-3 py-2 text-sm text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-indigo-600"
          value={values.title}
          onChange={(e) => onChange({ ...values, title: e.target.value })}
          required
        />
        {errors.title && <span className="block text-xs font-medium text-red-600">{errors.title}</span>}
      </label>
      <label className="block space-y-1.5">
        <span className="block text-sm font-medium text-gray-800">Description</span>
        <textarea
          className="block w-full rounded-md border-0 bg-white px-3 py-2 text-sm text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-indigo-600"
          rows={4}
          value={values.description}
          onChange={(e) => onChange({ ...values, description: e.target.value })}
        />
      </label>
      <div className="grid gap-4 sm:grid-cols-2">
        <label className="block space-y-1.5">
          <span className="block text-sm font-medium text-gray-800">Code</span>
          <input
            className="block w-full rounded-md border-0 bg-white px-3 py-2 text-sm text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-indigo-600"
            value={values.code}
            onChange={(e) => onChange({ ...values, code: e.target.value })}
            required
          />
          {errors.code && <span className="block text-xs font-medium text-red-600">{errors.code}</span>}
        </label>
        <label className="block space-y-1.5">
          <span className="block text-sm font-medium text-gray-800">Term</span>
          <input
            className="block w-full rounded-md border-0 bg-white px-3 py-2 text-sm text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-indigo-600"
            placeholder="e.g. Fall 2026"
            value={values.term}
            onChange={(e) => onChange({ ...values, term: e.target.value })}
            required
          />
          {errors.term && <span className="block text-xs font-medium text-red-600">{errors.term}</span>}
        </label>
      </div>
    </>
  )
}
