import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useParams, useSearchParams } from 'react-router'
import { isApiError } from '../../api/errors'
import { useAuth } from '../../auth/context'
import { Card, PageHeader } from '../../components/Layout'
import { Button } from '../../components/Button'
import { Input, Textarea } from '../../components/FormField'
import { ConfirmDialog } from '../../components/Modal'
import { EmptyState, ErrorState, ForbiddenState, Skeleton } from '../../components/States'
import { Pagination } from '../../components/Pagination'
import { useEnrollCourse } from '../courses/queries'
import { postAuthorId, postAuthorName } from './api'
import type { DiscussionPost, DiscussionReply } from './api'
import { useCreatePost, useDeleteDiscussion, useDiscussion, useReplyToPost, useUpdateDiscussion } from './queries'

function formatDate(value?: string): string {
  if (!value) return ''
  try {
    return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
  } catch {
    return value
  }
}

function mutationErrorMessage(error: unknown): string {
  if (isApiError(error) && error.status === 403) return 'You do not have permission to do that.'
  if (isApiError(error)) return error.message
  return 'Something went wrong. Please try again.'
}

interface EditValues {
  title?: string
  body: string
}

function EditComposer({
  initialTitle,
  initialBody,
  withTitle,
  submitting,
  error,
  onSubmit,
  onCancel,
}: {
  initialTitle: string
  initialBody: string
  withTitle: boolean
  submitting: boolean
  error?: string | null
  onSubmit: (values: EditValues) => void
  onCancel: () => void
}) {
  const [title, setTitle] = useState(initialTitle)
  const [body, setBody] = useState(initialBody)
  return (
    <form
      className="space-y-2"
      onSubmit={(e: FormEvent) => {
        e.preventDefault()
        if (!body.trim()) return
        onSubmit(withTitle ? { title: title.trim() || undefined, body: body.trim() } : { body: body.trim() })
      }}
    >
      {error && (
        <p className="text-xs font-medium text-red-600" role="alert">
          {error}
        </p>
      )}
      {withTitle && (
        <Input label="Title" name="editTitle" value={title} onChange={(e) => setTitle(e.target.value)} />
      )}
      <Textarea label="Body" name="editBody" rows={4} value={body} onChange={(e) => setBody(e.target.value)} required />
      <div className="flex justify-end gap-2">
        <Button variant="ghost" size="sm" onClick={onCancel} disabled={submitting}>
          Cancel
        </Button>
        <Button type="submit" size="sm" loading={submitting}>
          Save
        </Button>
      </div>
    </form>
  )
}

function ReplyComposer({
  submitting,
  error,
  onSubmit,
  onCancel,
}: {
  submitting: boolean
  error?: string | null
  onSubmit: (body: string) => void
  onCancel: () => void
}) {
  const [body, setBody] = useState('')
  return (
    <form
      className="space-y-2"
      onSubmit={(e: FormEvent) => {
        e.preventDefault()
        if (!body.trim()) return
        onSubmit(body.trim())
      }}
    >
      {error && (
        <p className="text-xs font-medium text-red-600" role="alert">
          {error}
        </p>
      )}
      <Textarea
        label="Your reply"
        name="replyBody"
        rows={3}
        value={body}
        onChange={(e) => setBody(e.target.value)}
        hint="Replies are one level deep — you cannot reply to a reply."
        required
      />
      <div className="flex justify-end gap-2">
        <Button variant="ghost" size="sm" onClick={onCancel} disabled={submitting}>
          Cancel
        </Button>
        <Button type="submit" size="sm" loading={submitting}>
          Reply
        </Button>
      </div>
    </form>
  )
}

function ReplyItem({
  reply,
  isAuthor,
  canDelete,
  editing,
  busy,
  onStartEdit,
  onCancelEdit,
  onSaveEdit,
  onDelete,
  error,
}: {
  reply: DiscussionReply
  isAuthor: boolean
  canDelete: boolean
  editing: boolean
  busy: boolean
  onStartEdit: () => void
  onCancelEdit: () => void
  onSaveEdit: (body: string) => void
  onDelete: () => void
  error?: string | null
}) {
  return (
    <div className="border-l-2 border-indigo-100 pl-4">
      <div className="flex items-center gap-2 text-xs text-gray-500">
        <span className="font-medium text-gray-700">{postAuthorName(reply)}</span>
        <span>{formatDate(reply.createdAt)}</span>
      </div>
      {editing ? (
        <div className="mt-2">
          <EditComposer
            initialTitle=""
            initialBody={reply.body}
            withTitle={false}
            submitting={busy}
            error={error}
            onSubmit={(values) => onSaveEdit(values.body)}
            onCancel={onCancelEdit}
          />
        </div>
      ) : (
        <>
          <p className="mt-1 whitespace-pre-wrap text-sm text-gray-700">{reply.body}</p>
          {(isAuthor || canDelete) && (
            <div className="mt-2 flex gap-3 text-xs font-medium">
              {isAuthor && (
                <button type="button" className="text-indigo-600 hover:text-indigo-500" onClick={onStartEdit}>
                  Edit
                </button>
              )}
              {canDelete && (
                <button
                  type="button"
                  className="text-red-600 hover:text-red-500"
                  onClick={onDelete}
                  disabled={busy}
                >
                  Delete
                </button>
              )}
            </div>
          )}
        </>
      )}
    </div>
  )
}

function PostItem({
  post,
  currentUserId,
  isAdmin,
  busy,
  onUpdate,
  onDelete,
  onReply,
}: {
  post: DiscussionPost
  currentUserId: number | string
  isAdmin: boolean
  busy: { update: boolean; delete: boolean; reply: boolean }
  onUpdate: (id: string | number, input: EditValues) => Promise<void>
  onDelete: (id: string | number) => void
  onReply: (postId: string | number, body: string) => Promise<void>
}) {
  const authorId = postAuthorId(post)
  const isAuthor = authorId !== null && String(authorId) === String(currentUserId)
  const canDelete = isAuthor || isAdmin

  const [mode, setMode] = useState<'view' | 'edit' | 'reply'>('view')
  const [error, setError] = useState<string | null>(null)
  const [editingReplyId, setEditingReplyId] = useState<string | null>(null)
  const [replyError, setReplyError] = useState<string | null>(null)

  const run = async (action: () => Promise<void>, onErrorState?: (msg: string | null) => void) => {
    setError(null)
    setReplyError(null)
    try {
      await action()
    } catch (e) {
      const msg = mutationErrorMessage(e)
      if (onErrorState) onErrorState(msg)
      else setError(msg)
    }
  }

  return (
    <Card>
      <div className="space-y-2">
        <div className="flex flex-wrap items-center gap-2">
          {post.title && <h3 className="text-sm font-semibold text-gray-900">{post.title}</h3>}
          <span className="text-xs font-medium text-gray-600">{postAuthorName(post)}</span>
          <span className="text-xs text-gray-400">{formatDate(post.createdAt)}</span>
        </div>

        {mode === 'edit' ? (
          <EditComposer
            initialTitle={post.title ?? ''}
            initialBody={post.body}
            withTitle
            submitting={busy.update}
            error={error}
            onSubmit={(values) => {
              void run(async () => {
                await onUpdate(post.id, values)
                setMode('view')
              })
            }}
            onCancel={() => setMode('view')}
          />
        ) : (
          <p className="whitespace-pre-wrap text-sm text-gray-700">{post.body}</p>
        )}

        {error && mode !== 'edit' && mode !== 'reply' && (
          <p className="text-xs font-medium text-red-600" role="alert">
            {error}
          </p>
        )}

        {mode === 'view' && (isAuthor || canDelete) && (
          <div className="flex gap-3 pt-1 text-xs font-medium">
            {isAuthor && (
              <button type="button" className="text-indigo-600 hover:text-indigo-500" onClick={() => setMode('edit')}>
                Edit
              </button>
            )}
            {canDelete && (
              <button
                type="button"
                className="text-red-600 hover:text-red-500"
                onClick={() => onDelete(post.id)}
                disabled={busy.delete}
              >
                Delete
              </button>
            )}
          </div>
        )}

        {/* Reply control renders only here — never on a reply (§7.8.3). */}
        {mode === 'view' && (
          <div className="pt-1">
            <Button variant="ghost" size="sm" onClick={() => setMode('reply')}>
              Reply
            </Button>
          </div>
        )}

        {mode === 'reply' && (
          <ReplyComposer
            submitting={busy.reply}
            error={replyError}
            onSubmit={(body) => {
              void run(async () => {
                await onReply(post.id, body)
                setMode('view')
              }, setReplyError)
            }}
            onCancel={() => setMode('view')}
          />
        )}

        {(post.replies ?? []).length > 0 && (
          <div className="space-y-4 border-t border-gray-100 pt-4">
            {(post.replies ?? []).map((reply) => {
              const replyAuthorId = postAuthorId(reply)
              const replyIsAuthor = replyAuthorId !== null && String(replyAuthorId) === String(currentUserId)
              return (
                <ReplyItem
                  key={String(reply.id)}
                  reply={reply}
                  isAuthor={replyIsAuthor}
                  canDelete={replyIsAuthor || isAdmin}
                  editing={editingReplyId === String(reply.id)}
                  busy={busy.update || busy.delete}
                  error={error}
                  onStartEdit={() => {
                    setError(null)
                    setEditingReplyId(String(reply.id))
                  }}
                  onCancelEdit={() => setEditingReplyId(null)}
                  onSaveEdit={(body) => {
                    void run(async () => {
                      await onUpdate(reply.id, { body })
                      setEditingReplyId(null)
                    })
                  }}
                  onDelete={() => onDelete(reply.id)}
                />
              )
            })}
          </div>
        )}
      </div>
    </Card>
  )
}

export function DiscussionScreen() {
  const { id: courseId = '' } = useParams()
  const { user } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const page = Number(searchParams.get('page') ?? '0') || 0

  const discussionQuery = useDiscussion(courseId, { page })
  const createPost = useCreatePost(courseId)
  const replyToPost = useReplyToPost(courseId)
  const updateDiscussion = useUpdateDiscussion(courseId)
  const deleteDiscussion = useDeleteDiscussion(courseId)
  const enrollCourse = useEnrollCourse()

  const isStudent = user?.role === 'STUDENT'
  const enrolled = isStudent && (user?.enrolledCourses ?? []).some((c) => String(c.id) === String(courseId))

  const [newTitle, setNewTitle] = useState('')
  const [newBody, setNewBody] = useState('')
  const [composerError, setComposerError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null)
  const [enrollMessage, setEnrollMessage] = useState<string | null>(null)

  if (discussionQuery.isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-1/4" />
        <Skeleton className="h-40 w-full" />
        <Skeleton className="h-40 w-full" />
      </div>
    )
  }

  if (discussionQuery.isError) {
    const error = discussionQuery.error
    if (isApiError(error) && error.status === 403) {
      return (
        <ForbiddenState
          message={
            isStudent && !enrolled
              ? 'You are not enrolled in this course.'
              : 'You do not have access to this discussion.'
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
                      await discussionQuery.refetch()
                    } catch (e) {
                      setEnrollMessage(
                        isApiError(e) && e.status === 409 ? 'Already enrolled in this course.' : 'Could not enroll.',
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
    return <ErrorState onRetry={() => void discussionQuery.refetch()} />
  }

  const data = discussionQuery.data
  const posts = data?.content ?? []

  const onComposerSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setComposerError(null)
    if (!newBody.trim()) {
      setComposerError('Body is required.')
      return
    }
    try {
      await createPost.mutateAsync({ title: newTitle.trim() || undefined, body: newBody.trim() })
      setNewTitle('')
      setNewBody('')
    } catch (error) {
      setComposerError(mutationErrorMessage(error))
    }
  }

  const handleUpdate = async (id: string | number, input: EditValues) => {
    try {
      await updateDiscussion.mutateAsync({ id, input })
      setActionError(null)
    } catch (error) {
      setActionError(mutationErrorMessage(error))
      throw error
    }
  }

  const handleReply = async (postId: string | number, body: string) => {
    try {
      await replyToPost.mutateAsync({ postId, body })
      setActionError(null)
    } catch (error) {
      setActionError(mutationErrorMessage(error))
      throw error
    }
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Discussion"
        description="Ask questions and join the conversation. Replies are one level deep."
        actions={
          <>
            {enrollMessage && <span className="text-sm text-amber-700">{enrollMessage}</span>}
            <Link to={`/courses/${courseId}`}>
              <Button variant="ghost">Back to course</Button>
            </Link>
          </>
        }
      />

      {actionError && (
        <div className="rounded-md bg-red-50 px-3 py-2 text-sm font-medium text-red-700" role="alert">
          {actionError}
        </div>
      )}

      <Card>
        <form onSubmit={onComposerSubmit} className="space-y-3" noValidate>
          <h2 className="text-sm font-semibold text-gray-900">Start a thread</h2>
          {composerError && (
            <div className="rounded-md bg-red-50 px-3 py-2 text-sm font-medium text-red-700" role="alert">
              {composerError}
            </div>
          )}
          <Input
            label="Title (optional)"
            name="postTitle"
            value={newTitle}
            onChange={(e) => setNewTitle(e.target.value)}
          />
          <Textarea
            label="Body"
            name="postBody"
            rows={4}
            value={newBody}
            onChange={(e) => setNewBody(e.target.value)}
            required
          />
          <div className="flex justify-end">
            <Button type="submit" loading={createPost.isPending}>
              Post
            </Button>
          </div>
        </form>
      </Card>

      {posts.length === 0 ? (
        <EmptyState title="No posts yet" description="Be the first to start a discussion." />
      ) : (
        <div className="space-y-4">
          {posts.map((post) => (
            <PostItem
              key={String(post.id)}
              post={post}
              currentUserId={user?.id ?? ''}
              isAdmin={user?.role === 'ADMIN'}
              busy={{
                update: updateDiscussion.isPending,
                delete: deleteDiscussion.isPending,
                reply: replyToPost.isPending,
              }}
              onUpdate={handleUpdate}
              onDelete={(id) => setConfirmDeleteId(String(id))}
              onReply={handleReply}
            />
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

      <ConfirmDialog
        open={confirmDeleteId !== null}
        title="Delete post"
        message="Delete this post? This cannot be undone."
        confirmLabel="Delete"
        destructive
        loading={deleteDiscussion.isPending}
        onConfirm={() => {
          if (!confirmDeleteId) return
          const id = confirmDeleteId
          setConfirmDeleteId(null)
          deleteDiscussion.mutate(id, {
            onError: (error) => setActionError(mutationErrorMessage(error)),
          })
        }}
        onCancel={() => setConfirmDeleteId(null)}
      />
    </div>
  )
}
