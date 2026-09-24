import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { isApiError } from '../../api/errors'
import { queryKeys } from '../../api/keys'
import { useAuth } from '../../auth/context'
import { PageHeader } from '../../components/Layout'
import { Button } from '../../components/Button'
import { Markdown } from '../../components/Markdown'
import { ErrorState, ForbiddenState, Skeleton } from '../../components/States'
import { getContent } from '../content/api'
import { useEnrollCourse } from '../courses/queries'
import { useSectionContent, useSections } from './queries'

function ContentBody({ contentId }: { contentId: string }) {
  const query = useQuery({
    queryKey: queryKeys.content(contentId),
    queryFn: () => getContent(contentId),
  })

  if (query.isLoading) return <Skeleton className="h-32 w-full" />
  if (query.isError) return <ErrorState onRetry={() => void query.refetch()} />
  if (!query.data) return null

  return (
    <div className="border-l-2 border-indigo-200 pl-4">
      <h3 className="text-sm font-semibold text-gray-900">{query.data.title}</h3>
      <div className="mt-2">
        <Markdown>{query.data.body}</Markdown>
      </div>
    </div>
  )
}

export function ContentReaderScreen() {
  const { id: courseId = '' } = useParams()
  const { user } = useAuth()
  const sectionsQuery = useSections(courseId)
  const enrollCourse = useEnrollCourse()

  const [openSection, setOpenSection] = useState<string | null>(null)
  const [openContent, setOpenContent] = useState<string | null>(null)
  const [enrollMessage, setEnrollMessage] = useState<string | null>(null)

  const isStudent = user?.role === 'STUDENT'
  const enrolled =
    isStudent && (user?.enrolledCourses ?? []).some((c) => String(c.id) === String(courseId))

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
            isStudent && !enrolled
              ? 'You are not enrolled in this course.'
              : 'You do not have access to this course content.'
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
                      await sectionsQuery.refetch()
                    } catch (e) {
                      setEnrollMessage(isApiError(e) && e.status === 409 ? 'Already enrolled in this course.' : 'Could not enroll.')
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
    return <ErrorState onRetry={() => void sectionsQuery.refetch()} />
  }

  const sections = sectionsQuery.data ?? []

  return (
    <div className="space-y-6">
      <PageHeader
        title="Course content"
        description="Sections and reading materials, in order."
        actions={enrollMessage ? <span className="text-sm text-amber-700">{enrollMessage}</span> : undefined}
      />

      {sections.length === 0 ? (
        <ForbiddenState message="No sections have been published yet." />
      ) : (
        <div className="space-y-3">
          {sections.map((section) => {
            const expanded = openSection === String(section.id)
            return (
              <div key={String(section.id)} className="overflow-hidden rounded-xl border border-gray-200 bg-white">
                <button
                  type="button"
                  onClick={() => {
                    setOpenSection(expanded ? null : String(section.id))
                    setOpenContent(null)
                  }}
                  className="flex w-full items-center justify-between px-5 py-4 text-left hover:bg-gray-50"
                  aria-expanded={expanded}
                >
                  <span className="text-sm font-semibold text-gray-900">
                    <span className="mr-2 text-gray-400">{section.orderIndex + 1}.</span>
                    {section.title}
                  </span>
                  <span className="text-gray-400">{expanded ? '−' : '+'}</span>
                </button>
                {expanded && <SectionContent sectionId={String(section.id)} openContent={openContent} onOpenContent={setOpenContent} />}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

function SectionContent({
  sectionId,
  openContent,
  onOpenContent,
}: {
  sectionId: string
  openContent: string | null
  onOpenContent: (id: string | null) => void
}) {
  const contentQuery = useSectionContent(sectionId)

  if (contentQuery.isLoading) {
    return (
      <div className="space-y-2 border-t border-gray-100 px-5 py-4">
        <Skeleton className="h-8 w-full" />
        <Skeleton className="h-8 w-full" />
      </div>
    )
  }
  if (contentQuery.isError) {
    return (
      <div className="border-t border-gray-100 px-5 py-4">
        <ErrorState onRetry={() => void contentQuery.refetch()} />
      </div>
    )
  }

  const items = contentQuery.data ?? []
  if (items.length === 0) {
    return (
      <p className="border-t border-gray-100 px-5 py-4 text-sm text-gray-500">No content in this section yet.</p>
    )
  }

  return (
    <div className="divide-y divide-gray-100 border-t border-gray-100">
      {items.map((item) => {
        const itemOpen = openContent === String(item.id)
        return (
          <div key={String(item.id)}>
            <button
              type="button"
              onClick={() => onOpenContent(itemOpen ? null : String(item.id))}
              className="flex w-full items-center justify-between px-5 py-3 text-left text-sm hover:bg-gray-50"
              aria-expanded={itemOpen}
            >
              <span className="font-medium text-gray-800">{item.title}</span>
              <span className="text-xs text-gray-400">{itemOpen ? 'Hide' : 'Read'}</span>
            </button>
            {itemOpen && (
              <div className="px-5 pb-4">
                <ContentBody contentId={String(item.id)} />
              </div>
            )}
          </div>
        )
      })}
    </div>
  )
}
