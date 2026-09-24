import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useParams, useSearchParams } from 'react-router'
import { isApiError } from '../../api/errors'
import { useAuth } from '../../auth/context'
import { Card, PageHeader } from '../../components/Layout'
import { Button } from '../../components/Button'
import { Input, Textarea } from '../../components/FormField'
import { ConfirmDialog, Modal } from '../../components/Modal'
import { EmptyState, ErrorState, ForbiddenState, Skeleton } from '../../components/States'
import { Pagination } from '../../components/Pagination'
import { formatDate } from '../../components/formatDate'
import { useEnrollCourse, useCourse } from '../courses/queries'
import type { Announcement } from './api'
import {
  useAnnouncements,
  useCreateAnnouncement,
  useDeleteAnnouncement,
  useUpdateAnnouncement,
} from './queries'

function mutationErrorMessage(error: unknown): string {
  if (isApiError(error) && error.status === 403) return 'You do not have permission to do that.'
  if (isApiError(error)) return error.message
  return 'Something went wrong. Please try again.'
}

function AnnouncementForm({
  initial,
  submitting,
  error,
  onSubmit,
  onCancel,
  submitLabel,
}: {
  initial?: { title: string; body: string }
  submitting: boolean
  error?: string | null
  onSubmit: (values: { title: string; body: string }) => void
  onCancel: () => void
  submitLabel: string
}) {
  const [title, setTitle] = useState(initial?.title ?? '')
  const [body, setBody] = useState(initial?.body ?? '')
  const [fieldErrors, setFieldErrors] = useState<{ title?: string; body?: string }>({})

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    const next: typeof fieldErrors = {}
    if (!title.trim()) next.title = 'Title is required.'
    if (!body.trim()) next.body = 'Body is required.'
    setFieldErrors(next)
    if (Object.keys(next).length > 0) return
    onSubmit({ title: title.trim(), body: body.trim() })
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4" noValidate>
      {error && (
        <div className="rounded-md bg-red-50 px-3 py-2 text-sm font-medium text-red-700" role="alert">
          {error}
        </div>
      )}
      <Input
        label="Title"
        name="announcementTitle"
        value={title}
        onChange={(e) => setTitle(e.target.value)}
        error={fieldErrors.title}
        required
      />
      <Textarea
        label="Body"
        name="announcementBody"
        rows={6}
        value={body}
        onChange={(e) => setBody(e.target.value)}
        error={fieldErrors.body}
        required
      />
      <div className="flex justify-end gap-3">
        <Button variant="secondary" onClick={onCancel} disabled={submitting}>
          Cancel
        </Button>
        <Button type="submit" loading={submitting}>
          {submitLabel}
        </Button>
      </div>
    </form>
  )
}

export function AnnouncementsScreen() {
  const { id: courseId = '' } = useParams()
  const { user } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const page = Number(searchParams.get('page') ?? '0') || 0

  const courseQuery = useCourse(courseId)
  const announcementsQuery = useAnnouncements(courseId, { page })
  const createAnnouncement = useCreateAnnouncement(courseId)
  const updateAnnouncement = useUpdateAnnouncement(courseId)
  const deleteAnnouncement = useDeleteAnnouncement(courseId)
  const enrollCourse = useEnrollCourse()

  const course = courseQuery.data
  // Own(Instructor) resolves through the course's instructor id (frontend-build §3).
  const isInstructorOwner =
    user?.role === 'INSTRUCTOR' &&
    course?.instructorId !== undefined &&
    course?.instructorId !== null &&
    String(course.instructorId) === String(user.id)
  const canManage = user?.role === 'ADMIN' || isInstructorOwner

  const isStudent = user?.role === 'STUDENT'
  const enrolled = isStudent && (user?.enrolledCourses ?? []).some((c) => String(c.id) === String(courseId))

  const [composerOpen, setComposerOpen] = useState(false)
  const [editing, setEditing] = useState<Announcement | null>(null)
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [enrollMessage, setEnrollMessage] = useState<string | null>(null)

  if (announcementsQuery.isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-1/4" />
        <Skeleton className="h-32 w-full" />
        <Skeleton className="h-32 w-full" />
      </div>
    )
  }

  if (announcementsQuery.isError) {
    const error = announcementsQuery.error
    if (isApiError(error) && error.status === 403) {
      return (
        <ForbiddenState
          message={
            isStudent && !enrolled
              ? 'You are not enrolled in this course.'
              : 'You do not have access to these announcements.'
          }
          action={
            <div className="flex gap-3">
              <Link to={`/courses/${courseId}`} className="text-sm font-medium text-indigo-600 hover:text-indigo-500">
                Back to course
              </Link>
              {isStudent && !enrolled && (
                <Button
                  size="sm"
                  loading={enrollCourse.isPending}
                  onClick={async () => {
                    try {
                      await enrollCourse.mutateAsync(courseId)
                      setEnrollMessage(null)
                      await announcementsQuery.refetch()
                    } catch (e) {
                      setEnrollMessage(
                        isApiError(e) && e.status === 409
                          ? 'Already enrolled in this course.'
                          : 'Could not enroll.',
                      )
                    }
                  }}
                >
                  Enroll
                </Button>
              )}
            </div>
          }
        />
      )
    }
    return <ErrorState onRetry={() => void announcementsQuery.refetch()} />
  }

  const data = announcementsQuery.data
  const announcements = data?.content ?? []

  const onCreate = async (values: { title: string; body: string }) => {
    try {
      await createAnnouncement.mutateAsync(values)
      setComposerOpen(false)
      setActionError(null)
    } catch (error) {
      return mutationErrorMessage(error)
    }
  }

  const onUpdate = async (values: { title: string; body: string }) => {
    if (!editing) return null
    try {
      await updateAnnouncement.mutateAsync({ id: editing.id, input: values })
      setEditing(null)
      setActionError(null)
      return null
    } catch (error) {
      return mutationErrorMessage(error)
    }
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Announcements"
        description={canManage ? 'Publish updates to everyone in this course.' : 'Updates from your instructor.'}
        actions={
          <>
            {enrollMessage && <span className="text-sm text-amber-700">{enrollMessage}</span>}
            <Link to={`/courses/${courseId}`}>
              <Button variant="ghost">Back to course</Button>
            </Link>
            {canManage && (
              <Button onClick={() => setComposerOpen(true)}>New announcement</Button>
            )}
          </>
        }
      />

      {actionError && (
        <div className="rounded-md bg-red-50 px-3 py-2 text-sm font-medium text-red-700" role="alert">
          {actionError}
        </div>
      )}

      {announcements.length === 0 ? (
        <EmptyState
          title="No announcements yet"
          description={canManage ? 'Publish the first announcement.' : undefined}
          action={
            canManage ? (
              <Button size="sm" onClick={() => setComposerOpen(true)}>
                New announcement
              </Button>
            ) : undefined
          }
        />
      ) : (
        <div className="space-y-4">
          {announcements.map((item) => (
            <Card key={String(item.id)}>
              <div className="space-y-2">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="flex flex-wrap items-center gap-2">
                    <h3 className="text-sm font-semibold text-gray-900">{item.title}</h3>
                    <span className="text-xs text-gray-400">{formatDate(item.createdAt)}</span>
                  </div>
                  {canManage && (
                    <div className="flex gap-3 text-xs font-medium">
                      <button
                        type="button"
                        className="text-indigo-600 hover:text-indigo-500"
                        onClick={() => setEditing(item)}
                      >
                        Edit
                      </button>
                      <button
                        type="button"
                        className="text-red-600 hover:text-red-500"
                        onClick={() => setConfirmDeleteId(String(item.id))}
                      >
                        Delete
                      </button>
                    </div>
                  )}
                </div>
                <p className="whitespace-pre-wrap text-sm text-gray-700">{item.body}</p>
              </div>
            </Card>
          ))}
        </div>
      )}

      {data && (
        <div className="rounded-xl border border-gray-200 bg-white px-6 py-4">
          <Pagination
            page={data.number}
            totalPages={data.totalPages}
            totalElements={data.totalElements}
            onPageChange={(next) => {
              const params = new URLSearchParams(searchParams)
              params.set('page', String(next))
              setSearchParams(params)
            }}
          />
        </div>
      )}

      <Modal open={composerOpen} title="New announcement" onClose={() => setComposerOpen(false)}>
        <AnnouncementForm
          submitting={createAnnouncement.isPending}
          submitLabel="Publish"
          onSubmit={async (values) => {
            const error = await onCreate(values)
            if (error) setActionError(error)
          }}
          onCancel={() => setComposerOpen(false)}
        />
      </Modal>

      <Modal open={editing !== null} title="Edit announcement" onClose={() => setEditing(null)}>
        {editing && (
          <AnnouncementForm
            initial={{ title: editing.title, body: editing.body }}
            submitting={updateAnnouncement.isPending}
            submitLabel="Save changes"
            onSubmit={async (values) => {
              const error = await onUpdate(values)
              if (error) setActionError(error)
            }}
            onCancel={() => setEditing(null)}
          />
        )}
      </Modal>

      <ConfirmDialog
        open={confirmDeleteId !== null}
        title="Delete announcement"
        message="Delete this announcement? This cannot be undone."
        confirmLabel="Delete"
        destructive
        loading={deleteAnnouncement.isPending}
        onConfirm={() => {
          if (!confirmDeleteId) return
          const id = confirmDeleteId
          setConfirmDeleteId(null)
          deleteAnnouncement.mutate(id, {
            onError: (error) => setActionError(mutationErrorMessage(error)),
          })
        }}
        onCancel={() => setConfirmDeleteId(null)}
      />
    </div>
  )
}
