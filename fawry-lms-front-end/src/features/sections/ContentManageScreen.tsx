import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import { isApiError } from '../../api/errors'
import { useAuth } from '../../auth/context'
import { PageHeader } from '../../components/Layout'
import { Button } from '../../components/Button'
import { Input, Textarea } from '../../components/FormField'
import { ConfirmDialog } from '../../components/Modal'
import { Markdown } from '../../components/Markdown'
import { ErrorState, ForbiddenState, Skeleton } from '../../components/States'
import { useQuery } from '@tanstack/react-query'
import { queryKeys } from '../../api/keys'
import { getContent } from '../content/api'
import type { ContentItem } from '../content/api'
import {
  useCreateContent,
  useCreateSection,
  useDeleteContent,
  useDeleteSection,
  useSectionContent,
  useSections,
  useUpdateContent,
  useUpdateSection,
} from './queries'
import type { Section } from './api'

function ContentEditor({
  initial,
  submitting,
  error,
  onSubmit,
  onCancel,
}: {
  initial?: ContentItem
  submitting: boolean
  error?: string
  onSubmit: (values: { title: string; body: string }) => void
  onCancel: () => void
}) {
  const [title, setTitle] = useState(initial?.title ?? '')
  const [body, setBody] = useState(initial?.body ?? '')
  const [localError, setLocalError] = useState<string | null>(null)

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    if (!title.trim()) {
      setLocalError('Title is required.')
      return
    }
    if (!body.trim()) {
      setLocalError('Body is required.')
      return
    }
    setLocalError(null)
    onSubmit({ title: title.trim(), body })
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4" noValidate>
      {(localError || error) && (
        <div className="rounded-md bg-red-50 px-3 py-2 text-sm font-medium text-red-700" role="alert">
          {localError ?? error}
        </div>
      )}
      <Input label="Title" name="contentTitle" value={title} onChange={(e) => setTitle(e.target.value)} required />
      <div className="grid gap-4 lg:grid-cols-2">
        <Textarea
          label="Markdown body"
          name="contentBody"
          rows={12}
          value={body}
          onChange={(e) => setBody(e.target.value)}
          placeholder="Write Markdown…"
          required
        />
        <div className="space-y-1.5">
          <span className="block text-sm font-medium text-gray-800">Preview</span>
          <div className="min-h-48 rounded-md border border-gray-200 bg-gray-50 p-3">
            {body.trim() ? (
              <Markdown>{body}</Markdown>
            ) : (
              <p className="text-sm text-gray-400">Nothing to preview.</p>
            )}
          </div>
        </div>
      </div>
      <div className="flex justify-end gap-3">
        <Button variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" loading={submitting}>
          {initial ? 'Save content' : 'Add content'}
        </Button>
      </div>
    </form>
  )
}

export function ContentManageScreen() {
  const { id: courseId = '' } = useParams()
  const { user } = useAuth()

  const sectionsQuery = useSections(courseId)
  const createSection = useCreateSection(courseId)
  const updateSection = useUpdateSection(courseId)
  const deleteSection = useDeleteSection(courseId)
  const deleteContent = useDeleteContent(courseId)

  const [openSection, setOpenSection] = useState<string | null>(null)
  const [newSectionTitle, setNewSectionTitle] = useState('')
  const [sectionError, setSectionError] = useState<string | null>(null)
  const [deletingSection, setDeletingSection] = useState<Section | null>(null)
  const [deletingContent, setDeletingContent] = useState<ContentItem | null>(null)

  const [editingContent, setEditingContent] = useState<ContentItem | null>(null)
  const [creatingContentFor, setCreatingContentFor] = useState<string | null>(null)
  const [contentError, setContentError] = useState<string | null>(null)

  const canManage = user?.role === 'ADMIN' || user?.role === 'INSTRUCTOR'

  const onAddSection = async (event: FormEvent) => {
    event.preventDefault()
    if (!newSectionTitle.trim()) {
      setSectionError('Title is required.')
      return
    }
    setSectionError(null)
    try {
      await createSection.mutateAsync(newSectionTitle.trim())
      setNewSectionTitle('')
    } catch (error) {
      setSectionError(isApiError(error) ? error.message : 'Something went wrong.')
    }
  }

  const onMove = async (section: Section, direction: -1 | 1) => {
    const sections = sectionsQuery.data ?? []
    const index = sections.findIndex((s) => String(s.id) === String(section.id))
    const target = sections[index + direction]
    if (!target) return
    try {
      await updateSection.mutateAsync({
        id: String(section.id),
        title: section.title,
        orderIndex: target.orderIndex,
      })
      await updateSection.mutateAsync({
        id: String(target.id),
        title: target.title,
        orderIndex: section.orderIndex,
      })
      await sectionsQuery.refetch()
    } catch {
      await sectionsQuery.refetch()
    }
  }

  if (sectionsQuery.isLoading) {
    return (
      <div className="space-y-3">
        <Skeleton className="h-8 w-1/3" />
        <Skeleton className="h-12 w-full" />
        <Skeleton className="h-12 w-full" />
      </div>
    )
  }

  if (sectionsQuery.isError) {
    const error = sectionsQuery.error
    if (isApiError(error) && error.status === 403) {
      return (
        <ForbiddenState
          message={
            user?.role === 'STUDENT'
              ? 'You are not enrolled in this course.'
              : 'You do not manage this course.'
          }
          action={
            <Link to={`/courses/${courseId}`} className="text-sm font-medium text-indigo-600">
              Back to course
            </Link>
          }
        />
      )
    }
    return <ErrorState onRetry={() => void sectionsQuery.refetch()} />
  }

  if (!canManage) {
    return <ForbiddenState message="Only the course instructor or an admin can manage content." />
  }

  const sections = sectionsQuery.data ?? []

  return (
    <div className="space-y-6">
      <PageHeader
        title="Manage content"
        description="Create sections, reorder them, and write Markdown content."
        actions={
          <Link to={`/courses/${courseId}/content`}>
            <Button variant="secondary">Reader view</Button>
          </Link>
        }
      />

      <form onSubmit={onAddSection} className="flex flex-wrap items-end gap-3 rounded-xl border border-gray-200 bg-white p-4">
        <div className="min-w-56 flex-1">
          <Input
            label="New section title"
            name="newSection"
            value={newSectionTitle}
            onChange={(e) => setNewSectionTitle(e.target.value)}
            error={sectionError ?? undefined}
          />
        </div>
        <Button type="submit" loading={createSection.isPending}>
          Add section
        </Button>
      </form>

      {sections.length === 0 ? (
        <ForbiddenState message="No sections yet — create the first one above." />
      ) : (
        <div className="space-y-3">
          {sections.map((section, index) => {
            const expanded = openSection === String(section.id)
            return (
              <div key={String(section.id)} className="overflow-hidden rounded-xl border border-gray-200 bg-white">
                <div className="flex items-center gap-2 px-4 py-3">
                  <button
                    type="button"
                    onClick={() => setOpenSection(expanded ? null : String(section.id))}
                    className="flex-1 text-left text-sm font-semibold text-gray-900 hover:text-indigo-700"
                    aria-expanded={expanded}
                  >
                    {index + 1}. {section.title}
                  </button>
                  <div className="flex items-center gap-1">
                    <Button
                      size="sm"
                      variant="ghost"
                      aria-label="Move up"
                      disabled={index === 0 || updateSection.isPending}
                      onClick={() => void onMove(section, -1)}
                    >
                      ↑
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      aria-label="Move down"
                      disabled={index === sections.length - 1 || updateSection.isPending}
                      onClick={() => void onMove(section, 1)}
                    >
                      ↓
                    </Button>
                    <Button size="sm" variant="danger" onClick={() => setDeletingSection(section)}>
                      Delete
                    </Button>
                  </div>
                </div>

                {expanded && (
                  <SectionManager
                    section={section}
                    courseId={courseId}
                    creatingContentFor={creatingContentFor}
                    setCreatingContentFor={setCreatingContentFor}
                    editingContent={editingContent}
                    setEditingContent={setEditingContent}
                    contentError={contentError}
                    setContentError={setContentError}
                    onRequestDeleteContent={setDeletingContent}
                  />
                )}
              </div>
            )
          })}
        </div>
      )}

      <ConfirmDialog
        open={deletingSection !== null}
        title="Delete section"
        message={`This will delete the section “${deletingSection?.title ?? ''}”.`}
        confirmLabel="Delete"
        destructive
        loading={deleteSection.isPending}
        onConfirm={async () => {
          if (!deletingSection) return
          try {
            await deleteSection.mutateAsync(String(deletingSection.id))
            setDeletingSection(null)
            setOpenSection(null)
          } catch {
            setDeletingSection(null)
          }
        }}
        onCancel={() => setDeletingSection(null)}
      />

      <ConfirmDialog
        open={deletingContent !== null}
        title="Delete content"
        message={`This will delete “${deletingContent?.title ?? ''}”.`}
        confirmLabel="Delete"
        destructive
        loading={deleteContent.isPending}
        onConfirm={async () => {
          if (!deletingContent) return
          try {
            await deleteContent.mutateAsync(String(deletingContent.id))
            setDeletingContent(null)
          } catch {
            setDeletingContent(null)
          }
        }}
        onCancel={() => setDeletingContent(null)}
      />
    </div>
  )
}

function SectionManager({
  section,
  courseId,
  creatingContentFor,
  setCreatingContentFor,
  editingContent,
  setEditingContent,
  contentError,
  setContentError,
  onRequestDeleteContent,
}: {
  section: Section
  courseId: string
  creatingContentFor: string | null
  setCreatingContentFor: (id: string | null) => void
  editingContent: ContentItem | null
  setEditingContent: (item: ContentItem | null) => void
  contentError: string | null
  setContentError: (message: string | null) => void
  onRequestDeleteContent: (item: ContentItem) => void
}) {
  const sectionId = String(section.id)
  const contentQuery = useSectionContent(sectionId)
  const createContent = useCreateContent(sectionId, courseId)
  const updateContent = useUpdateContent(courseId)

  const isCreating = creatingContentFor === sectionId
  const isEditing = editingContent !== null && String(editingContent.sectionId ?? '') === sectionId

  const editingFullQuery = useQuery({
    queryKey: queryKeys.content(String(editingContent?.id ?? '')),
    queryFn: () => getContent(String(editingContent!.id)),
    enabled: editingContent !== null,
  })

  const editorInitial = editingFullQuery.data ?? editingContent ?? undefined

  return (
    <div className="border-t border-gray-100">
      {contentQuery.isLoading ? (
        <div className="space-y-2 p-4">
          <Skeleton className="h-8 w-full" />
          <Skeleton className="h-8 w-full" />
        </div>
      ) : contentQuery.isError ? (
        <div className="p-4">
          <ErrorState onRetry={() => void contentQuery.refetch()} />
        </div>
      ) : (
        <ul className="divide-y divide-gray-100">
          {(contentQuery.data ?? []).map((item) => (
            <li key={String(item.id)} className="flex items-center justify-between px-4 py-2.5">
              <span className="text-sm text-gray-800">{item.title}</span>
              <div className="flex gap-2">
                <Button size="sm" variant="secondary" onClick={() => setEditingContent(item)}>
                  Edit
                </Button>
                <Button size="sm" variant="danger" onClick={() => onRequestDeleteContent(item)}>
                  Delete
                </Button>
              </div>
            </li>
          ))}
        </ul>
      )}

      <div className="p-4">
        {isEditing && editingContent ? (
          editingFullQuery.isLoading && !editorInitial ? (
            <Skeleton className="h-64 w-full" />
          ) : (
            <ContentEditor
              key={String(editingContent.id)}
              initial={editorInitial}
              submitting={updateContent.isPending}
              error={contentError ?? undefined}
              onSubmit={async (values) => {
                setContentError(null)
                try {
                  await updateContent.mutateAsync({ id: String(editingContent.id), input: values })
                  setEditingContent(null)
                } catch (error) {
                  setContentError(isApiError(error) ? error.message : 'Something went wrong.')
                }
              }}
              onCancel={() => {
                setEditingContent(null)
                setContentError(null)
              }}
            />
          )
        ) : isCreating ? (
          <ContentEditor
            submitting={createContent.isPending}
            error={contentError ?? undefined}
            onSubmit={async (values) => {
              setContentError(null)
              try {
                await createContent.mutateAsync(values)
                setCreatingContentFor(null)
              } catch (error) {
                setContentError(isApiError(error) ? error.message : 'Something went wrong.')
              }
            }}
            onCancel={() => {
              setCreatingContentFor(null)
              setContentError(null)
            }}
          />
        ) : (
          <Button variant="secondary" size="sm" onClick={() => setCreatingContentFor(sectionId)}>
            Add content
          </Button>
        )}
      </div>
    </div>
  )
}
