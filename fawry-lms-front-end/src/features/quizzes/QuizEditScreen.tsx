import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useLocation, useParams } from 'react-router'
import { isApiError } from '../../api/errors'
import { Badge, Card, PageHeader } from '../../components/Layout'
import { Button } from '../../components/Button'
import { Input, Textarea } from '../../components/FormField'
import { ConfirmDialog } from '../../components/Modal'
import { ErrorState, ForbiddenState, Skeleton } from '../../components/States'
import type { QuestionInput, Quiz, StaffQuizQuestion } from './api'
import {
  useCreateQuestion,
  useDeleteQuestion,
  useQuiz,
  useQuizQuestions,
  useUpdateQuestion,
  useUpdateQuiz,
} from './queries'

interface DraftOption {
  text: string
  correct: boolean
}

function QuestionEditor({
  quiz,
  courseId,
  existing,
  nextOrderIndex,
  onDone,
}: {
  quiz: Quiz
  courseId: string
  existing?: StaffQuizQuestion
  nextOrderIndex: number
  onDone: () => void
}) {
  const createQuestion = useCreateQuestion(String(quiz.id), courseId)
  const updateQuestion = useUpdateQuestion(String(quiz.id), courseId)

  const [text, setText] = useState(existing?.text ?? '')
  const [options, setOptions] = useState<DraftOption[]>(
    existing?.options?.length
      ? existing.options.map((o) => ({ text: o.text ?? '', correct: o.isCorrect ?? false }))
      : [
          { text: '', correct: false },
          { text: '', correct: false },
        ],
  )
  const [error, setError] = useState<string | null>(null)

  const addOption = () => setOptions([...options, { text: '', correct: false }])

  const toggleCorrect = (index: number) => {
    setOptions(options.map((o, i) => (i === index ? { ...o, correct: !o.correct } : o)))
  }

  const setOptionText = (index: number, value: string) => {
    setOptions(options.map((o, i) => (i === index ? { ...o, text: value } : o)))
  }

  const removeOption = (index: number) => {
    if (options.length <= 2) return
    setOptions(options.filter((_, i) => i !== index))
  }

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault()
    const filled = options.filter((o) => o.text.trim())
    if (!text.trim()) {
      setError('Question text is required.')
      return
    }
    if (filled.length < 2) {
      setError('A question needs at least 2 options.')
      return
    }
    if (!filled.some((o) => o.correct)) {
      setError('Mark exactly one option as correct.')
      return
    }

    const payload: QuestionInput = {
      text: text.trim(),
      orderIndex: existing?.orderIndex ?? nextOrderIndex,
      options: filled.map((o) => ({ text: o.text.trim(), isCorrect: o.correct })),
    }

    setError(null)
    try {
      if (existing?.id !== undefined && existing?.id !== null) {
        await updateQuestion.mutateAsync({ questionId: String(existing.id), input: payload })
      } else {
        await createQuestion.mutateAsync(payload)
      }
      onDone()
    } catch (e) {
      if (isApiError(e) && e.fieldErrors) setError(Object.values(e.fieldErrors).join(' '))
      else if (isApiError(e)) setError(e.message)
      else setError('Something went wrong.')
    }
  }

  return (
    <form onSubmit={onSubmit} className="space-y-4 rounded-lg border border-gray-200 bg-gray-50 p-4" noValidate>
      {error && (
        <div className="rounded-md bg-red-50 px-3 py-2 text-sm font-medium text-red-700" role="alert">
          {error}
        </div>
      )}
      <Textarea
        label="Question"
        name="questionText"
        rows={2}
        value={text}
        onChange={(e) => setText(e.target.value)}
        required
      />

      <div className="space-y-2">
        <span className="block text-sm font-medium text-gray-800">
          Options <span className="font-normal text-gray-500">(exactly one correct answer)</span>
        </span>
        {options.map((option, index) => (
          <div key={index} className="flex items-center gap-2">
            <input
              type="checkbox"
              checked={option.correct}
              onChange={() => toggleCorrect(index)}
              className="size-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-600"
              aria-label={`Mark option ${index + 1} correct`}
            />
            <input
              value={option.text}
              onChange={(e) => setOptionText(index, e.target.value)}
              placeholder={`Option ${index + 1}`}
              className="block w-full rounded-md border-0 bg-white px-3 py-1.5 text-sm text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-indigo-600"
            />
            <Button
              size="sm"
              variant="ghost"
              onClick={() => removeOption(index)}
              disabled={options.length <= 2}
              aria-label={`Remove option ${index + 1}`}
            >
              ✕
            </Button>
          </div>
        ))}
        <Button size="sm" variant="secondary" onClick={addOption}>
          Add option
        </Button>
      </div>

      <div className="flex justify-end gap-3">
        <Button variant="secondary" onClick={onDone}>
          Cancel
        </Button>
        <Button type="submit" loading={createQuestion.isPending || updateQuestion.isPending}>
          {existing ? 'Save question' : 'Add question'}
        </Button>
      </div>
    </form>
  )
}

export function QuizEditScreen() {
  const { id: quizId = '' } = useParams()
  const location = useLocation()

  const quizQuery = useQuiz(quizId)
  const questionsQuery = useQuizQuestions(quizId)
  const quiz = quizQuery.data
  const questions = questionsQuery.data ?? []

  const stateCourseId = (location.state as { courseId?: string } | null)?.courseId
  const courseId = String(quiz?.courseId ?? stateCourseId ?? '')

  const updateQuiz = useUpdateQuiz(courseId)
  const deleteQuestion = useDeleteQuestion(quizId, courseId)

  const [title, setTitle] = useState('')
  const [duration, setDuration] = useState('')
  const [initialized, setInitialized] = useState(false)
  const [addingQuestion, setAddingQuestion] = useState(false)
  const [editingQuestion, setEditingQuestion] = useState<StaffQuizQuestion | null>(null)
  const [deletingQuestion, setDeletingQuestion] = useState<StaffQuizQuestion | null>(null)
  const [metaError, setMetaError] = useState<string | null>(null)

  if (quiz && !initialized) {
    setTitle(quiz.title)
    setDuration(String(quiz.durationMinutes))
    setInitialized(true)
  }

  if (quizQuery.isLoading || !initialized) {
    return (
      <div className="space-y-3">
        <Skeleton className="h-8 w-1/3" />
        <Skeleton className="h-40 w-full" />
      </div>
    )
  }

  if (quizQuery.isError) {
    const error = quizQuery.error
    if (isApiError(error) && error.status === 403) return <ForbiddenState message="You do not manage this quiz." />
    if (isApiError(error) && error.status === 404) return <ErrorState message="Quiz not found." />
    return <ErrorState onRetry={() => void quizQuery.refetch()} />
  }

  if (!quiz) return <ErrorState message="Quiz not found." />

  const nextOrderIndex =
    questions.length === 0
      ? 0
      : Math.max(...questions.map((q) => q.orderIndex ?? 0)) + 1

  const onSaveMeta = async (event: FormEvent) => {
    event.preventDefault()
    const durationMinutes = Number(duration)
    if (!title.trim()) {
      setMetaError('Title is required.')
      return
    }
    if (!Number.isInteger(durationMinutes) || durationMinutes < 1) {
      setMetaError('Duration must be a positive whole number of minutes.')
      return
    }
    setMetaError(null)
    try {
      await updateQuiz.mutateAsync({ id: quizId, input: { title: title.trim(), durationMinutes } })
    } catch (e) {
      setMetaError(isApiError(e) ? e.message : 'Something went wrong.')
    }
  }

  const onTogglePublish = async () => {
    try {
      await updateQuiz.mutateAsync({ id: quizId, input: { published: !quiz.published } })
    } catch {
      // reflected on refetch
    }
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <PageHeader
        title="Edit quiz"
        description={courseId ? undefined : 'Course id unavailable in payload — manage from the course quiz list.'}
        actions={
          <div className="flex items-center gap-2">
            {quiz.published ? <Badge color="green">Published</Badge> : <Badge color="amber">Draft</Badge>}
            <Button variant={quiz.published ? 'secondary' : 'primary'} onClick={onTogglePublish} loading={updateQuiz.isPending}>
              {quiz.published ? 'Unpublish' : 'Publish'}
            </Button>
            {courseId && (
              <Link to={`/courses/${courseId}/quizzes/manage`}>
                <Button variant="ghost">Back</Button>
              </Link>
            )}
          </div>
        }
      />

      <Card>
        <form onSubmit={onSaveMeta} className="space-y-4" noValidate>
          {metaError && (
            <div className="rounded-md bg-red-50 px-3 py-2 text-sm font-medium text-red-700" role="alert">
              {metaError}
            </div>
          )}
          <Input label="Title" name="quizTitle" value={title} onChange={(e) => setTitle(e.target.value)} required />
          <Input
            label="Duration (minutes)"
            name="durationMinutes"
            type="number"
            min={1}
            value={duration}
            onChange={(e) => setDuration(e.target.value)}
            required
          />
          <div className="flex justify-end">
            <Button type="submit" loading={updateQuiz.isPending}>
              Save details
            </Button>
          </div>
        </form>
      </Card>

      <Card>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-gray-900">
            Questions ({questions.length})
          </h2>
          {courseId && (
            <Button size="sm" onClick={() => { setAddingQuestion(true); setEditingQuestion(null) }}>
              Add question
            </Button>
          )}
        </div>

        <div className="space-y-4">
          {questionsQuery.isLoading && <Skeleton className="h-24 w-full" />}
          {questionsQuery.isError && (
            <ErrorState
              message={isApiError(questionsQuery.error) ? questionsQuery.error.message : 'Failed to load questions.'}
              onRetry={() => void questionsQuery.refetch()}
            />
          )}

          {addingQuestion && courseId && (
            <QuestionEditor
              quiz={quiz}
              courseId={courseId}
              nextOrderIndex={nextOrderIndex}
              onDone={() => setAddingQuestion(false)}
            />
          )}

          {questions.map((question, index) =>
            editingQuestion && String(editingQuestion.id) === String(question.id) && courseId ? (
              <QuestionEditor
                key={String(question.id)}
                quiz={quiz}
                courseId={courseId}
                existing={editingQuestion}
                nextOrderIndex={nextOrderIndex}
                onDone={() => setEditingQuestion(null)}
              />
            ) : (
              <div key={String(question.id ?? index)} className="rounded-lg border border-gray-200 p-4">
                <div className="flex items-start justify-between gap-3">
                  <p className="text-sm font-medium text-gray-900">
                    {index + 1}. {question.text}
                  </p>
                  <div className="flex shrink-0 gap-2">
                    <Button size="sm" variant="secondary" onClick={() => { setEditingQuestion(question); setAddingQuestion(false) }}>
                      Edit
                    </Button>
                    <Button size="sm" variant="danger" onClick={() => setDeletingQuestion(question)}>
                      Delete
                    </Button>
                  </div>
                </div>
                <ul className="mt-2 space-y-1">
                  {(question.options ?? []).map((option, optionIndex) => (
                    <li key={String(option.id ?? optionIndex)} className="flex items-center gap-2 text-sm text-gray-700">
                      <span
                        className={`inline-block size-2 rounded-full ${option.isCorrect ? 'bg-green-500' : 'bg-gray-300'}`}
                        aria-hidden="true"
                      />
                      {option.text}
                      {option.isCorrect && <span className="text-xs font-medium text-green-700">correct</span>}
                    </li>
                  ))}
                </ul>
              </div>
            ),
          )}

          {questions.length === 0 && !addingQuestion && !questionsQuery.isLoading && (
            <p className="text-sm text-gray-500">No questions yet. Add at least two options per question, with exactly one marked correct.</p>
          )}
        </div>
      </Card>

      <ConfirmDialog
        open={deletingQuestion !== null}
        title="Delete question"
        message="This will delete the question."
        confirmLabel="Delete"
        destructive
        loading={deleteQuestion.isPending}
        onConfirm={async () => {
          if (!deletingQuestion?.id) return
          try {
            await deleteQuestion.mutateAsync(String(deletingQuestion.id))
            setDeletingQuestion(null)
          } catch {
            setDeletingQuestion(null)
          }
        }}
        onCancel={() => setDeletingQuestion(null)}
      />
    </div>
  )
}
