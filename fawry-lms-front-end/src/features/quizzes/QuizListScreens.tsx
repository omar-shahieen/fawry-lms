import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import { isApiError } from '../../api/errors'
import { useAuth } from '../../auth/context'
import { Badge, Card, PageHeader } from '../../components/Layout'
import { Button } from '../../components/Button'
import { Input } from '../../components/FormField'
import { ConfirmDialog, Modal } from '../../components/Modal'
import { EmptyState, ErrorState, ForbiddenState, Skeleton } from '../../components/States'
import { useCourse } from '../courses/queries'
import { useCourseQuizzes, useCreateQuiz, useDeleteQuiz, useUpdateQuiz } from './queries'

export function CourseQuizzesScreen() {
  const { id: courseId = '' } = useParams()
  const { user } = useAuth()
  const quizzesQuery = useCourseQuizzes(courseId)
  const courseQuery = useCourse(courseId)

  const isStaff = user?.role === 'INSTRUCTOR' || user?.role === 'ADMIN'

  if (quizzesQuery.isLoading) {
    return (
      <div className="space-y-3">
        <Skeleton className="h-8 w-1/3" />
        <Skeleton className="h-20 w-full" />
        <Skeleton className="h-20 w-full" />
      </div>
    )
  }

  if (quizzesQuery.isError) {
    const error = quizzesQuery.error
    if (isApiError(error) && error.status === 403) {
      return (
        <ForbiddenState
          message={
            user?.role === 'STUDENT'
              ? 'You are not enrolled in this course.'
              : 'You do not have access to this course.'
          }
          action={
            <Link to={`/courses/${courseId}`} className="text-sm font-medium text-indigo-600">
              Back to course
            </Link>
          }
        />
      )
    }
    return <ErrorState onRetry={() => void quizzesQuery.refetch()} />
  }

  const quizzes = quizzesQuery.data ?? []

  return (
    <div className="space-y-6">
      <PageHeader
        title="Quizzes"
        description={isStaff ? 'All quizzes in this course.' : 'Published quizzes in this course.'}
        actions={
          <>
            <Link to={`/courses/${courseId}`}>
              <Button variant="ghost">Back to course</Button>
            </Link>
            {isStaff && (
              <Link to={`/courses/${courseId}/quizzes/manage`}>
                <Button>Manage quizzes</Button>
              </Link>
            )}
          </>
        }
      />

      {quizzes.length === 0 ? (
        <EmptyState
          title={isStaff ? 'No quizzes yet' : 'No quizzes available'}
          description={isStaff ? 'Create the first quiz from the manage screen.' : undefined}
        />
      ) : (
        <div className="space-y-3">
          {quizzes.map((quiz) => (
            <Card key={String(quiz.id)} className="flex items-center justify-between gap-4">
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-sm font-semibold text-gray-900">{quiz.title}</h2>
                  {quiz.published ? <Badge color="green">Published</Badge> : <Badge color="amber">Draft</Badge>}
                </div>
                <p className="mt-1 text-xs text-gray-500">{quiz.durationMinutes} minutes</p>
              </div>
              <div className="flex items-center gap-2">
                {isStaff ? (
                  <Link to={`/quizzes/${quiz.id}/edit`} state={{ courseId }}>
                    <Button size="sm" variant="secondary">
                      Edit
                    </Button>
                  </Link>
                ) : quiz.published ? (
                  <Link to={`/quizzes/${quiz.id}`}>
                    <Button size="sm">Open</Button>
                  </Link>
                ) : null}
              </div>
            </Card>
          ))}
        </div>
      )}
      {courseQuery.isError && isApiError(courseQuery.error) && courseQuery.error.status === 403 && (
        <ForbiddenState message="You do not have access to this course." />
      )}
    </div>
  )
}

export function QuizManageScreen() {
  const { id: courseId = '' } = useParams()
  const quizzesQuery = useCourseQuizzes(courseId)
  const createQuiz = useCreateQuiz(courseId)
  const updateQuiz = useUpdateQuiz(courseId)
  const deleteQuiz = useDeleteQuiz(courseId)

  const [createOpen, setCreateOpen] = useState(false)
  const [title, setTitle] = useState('')
  const [duration, setDuration] = useState('15')
  const [formErrors, setFormErrors] = useState<{ title?: string; duration?: string; form?: string }>({})
  const [deleting, setDeleting] = useState<{ id: string; title: string } | null>(null)

  const onCreate = async (event: FormEvent) => {
    event.preventDefault()
    const nextErrors: typeof formErrors = {}
    if (!title.trim()) nextErrors.title = 'Title is required.'
    const durationMinutes = Number(duration)
    if (!Number.isInteger(durationMinutes) || durationMinutes < 1) nextErrors.duration = 'Enter a positive number of minutes.'
    setFormErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) return

    try {
      await createQuiz.mutateAsync({ title: title.trim(), durationMinutes })
      setCreateOpen(false)
      setTitle('')
      setDuration('15')
    } catch (error) {
      if (isApiError(error) && error.fieldErrors) setFormErrors(error.fieldErrors)
      else if (isApiError(error)) setFormErrors({ form: error.message })
      else setFormErrors({ form: 'Something went wrong.' })
    }
  }

  const onTogglePublish = async (quiz: { id: number | string; title: string; durationMinutes: number; published: boolean }) => {
    try {
      await updateQuiz.mutateAsync({ id: String(quiz.id), input: { published: !quiz.published } })
    } catch {
      // error surfaced by refetch state; keep UI stable
    }
  }

  if (quizzesQuery.isLoading) {
    return (
      <div className="space-y-3">
        <Skeleton className="h-8 w-1/3" />
        <Skeleton className="h-20 w-full" />
      </div>
    )
  }

  if (quizzesQuery.isError) {
    const error = quizzesQuery.error
    if (isApiError(error) && error.status === 403) {
      return <ForbiddenState message="You do not manage this course's quizzes." />
    }
    return <ErrorState onRetry={() => void quizzesQuery.refetch()} />
  }

  const quizzes = quizzesQuery.data ?? []

  return (
    <div className="space-y-6">
      <PageHeader
        title="Manage quizzes"
        description="Create quizzes, add questions, publish."
        actions={
          <>
            <Link to={`/courses/${courseId}/quizzes`}>
              <Button variant="secondary">Quiz list</Button>
            </Link>
            <Button onClick={() => setCreateOpen(true)}>New quiz</Button>
          </>
        }
      />

      {quizzes.length === 0 ? (
        <EmptyState title="No quizzes yet" description="Create a quiz, then add questions to it." />
      ) : (
        <div className="space-y-3">
          {quizzes.map((quiz) => (
            <Card key={String(quiz.id)} className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-sm font-semibold text-gray-900">{quiz.title}</h2>
                  {quiz.published ? <Badge color="green">Published</Badge> : <Badge color="amber">Draft</Badge>}
                </div>
                <p className="mt-1 text-xs text-gray-500">
                  {quiz.durationMinutes} minutes · {quiz.questions?.length ?? 0} question
                  {(quiz.questions?.length ?? 0) === 1 ? '' : 's'}
                </p>
              </div>
              <div className="flex items-center gap-2">
                <Button
                  size="sm"
                  variant={quiz.published ? 'secondary' : 'primary'}
                  loading={updateQuiz.isPending}
                  onClick={() => void onTogglePublish(quiz)}
                >
                  {quiz.published ? 'Unpublish' : 'Publish'}
                </Button>
                <Link to={`/quizzes/${quiz.id}/edit`} state={{ courseId }}>
                  <Button size="sm" variant="secondary">
                    Edit
                  </Button>
                </Link>
                <Button size="sm" variant="danger" onClick={() => setDeleting({ id: String(quiz.id), title: quiz.title })}>
                  Delete
                </Button>
              </div>
            </Card>
          ))}
        </div>
      )}

      <Modal open={createOpen} title="Create quiz" onClose={() => setCreateOpen(false)}>
        <form onSubmit={onCreate} className="space-y-4" noValidate>
          {formErrors.form && (
            <div className="rounded-md bg-red-50 px-3 py-2 text-sm font-medium text-red-700" role="alert">
              {formErrors.form}
            </div>
          )}
          <Input
            label="Title"
            name="quizTitle"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            error={formErrors.title}
            required
          />
          <Input
            label="Duration (minutes)"
            name="durationMinutes"
            type="number"
            min={1}
            value={duration}
            onChange={(e) => setDuration(e.target.value)}
            error={formErrors.duration}
            required
          />
          <div className="flex justify-end">
            <Button type="submit" loading={createQuiz.isPending}>
              Create quiz
            </Button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={deleting !== null}
        title="Delete quiz"
        message={`Delete “${deleting?.title ?? ''}”? This cannot be undone.`}
        confirmLabel="Delete"
        destructive
        loading={deleteQuiz.isPending}
        onConfirm={async () => {
          if (!deleting) return
          try {
            await deleteQuiz.mutateAsync(deleting.id)
            setDeleting(null)
          } catch {
            setDeleting(null)
          }
        }}
        onCancel={() => setDeleting(null)}
      />
    </div>
  )
}
