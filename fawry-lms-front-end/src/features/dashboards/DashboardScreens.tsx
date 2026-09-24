import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { useAuth } from '../../auth/context'
import { Badge, Card, PageHeader } from '../../components/Layout'
import { Button } from '../../components/Button'
import { EmptyState, ErrorState, Skeleton } from '../../components/States'
import type { DashboardCourseRef, DashboardQuizStatus } from './api'
import { useAdminDashboard, useInstructorDashboard, useStudentDashboard } from './queries'

function quizTitle(quiz: DashboardQuizStatus): string {
  return quiz.title ?? quiz.quizTitle ?? 'Quiz'
}

function quizId(quiz: DashboardQuizStatus): number | string | null {
  return quiz.id ?? quiz.quizId ?? null
}

function isAttempted(quiz: DashboardQuizStatus): boolean {
  return quiz.attempted === true || quiz.hasAttempt === true || quiz.score !== undefined || quiz.bestScore !== undefined
}

function courseTitle(course: DashboardCourseRef): string {
  return course.title ?? 'Course'
}

function CourseLink({ course, children }: { course: DashboardCourseRef; children?: ReactNode }) {
  if (course.id === undefined || course.id === null) return <>{children ?? courseTitle(course)}</>
  return <Link to={`/courses/${course.id}`} className="font-medium text-indigo-600 hover:text-indigo-500">{children ?? courseTitle(course)}</Link>
}

function StudentDashboard() {
  const query = useStudentDashboard()

  if (query.isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-1/4" />
        <Skeleton className="h-32 w-full" />
        <Skeleton className="h-32 w-full" />
      </div>
    )
  }
  if (query.isError) return <ErrorState onRetry={() => void query.refetch()} />

  const data = query.data ?? {}
  const courses = data.enrolledCourses ?? data.courses ?? []
  const quizStatus = data.quizStatus ?? data.quizzes ?? []
  const courseQuizzes = data.courseQuizzes ?? []

  return (
    <div className="space-y-6">
      <PageHeader title="Dashboard" description="Your enrolled courses and quiz status." />

      <section className="space-y-3">
        <h2 className="text-sm font-semibold text-gray-900">Enrolled courses</h2>
        {courses.length === 0 ? (
          <EmptyState
            title="No courses yet"
            description="Browse the catalog to enroll."
            action={
              <Link to="/courses">
                <Button size="sm" variant="secondary">
                  Browse courses
                </Button>
              </Link>
            }
          />
        ) : (
          <div className="grid gap-3 sm:grid-cols-2">
            {courses.map((course, index) => (
              <Card key={String(course.id ?? index)}>
                <div className="flex items-center justify-between gap-2">
                  <CourseLink course={course} />
                  {course.code && <Badge>{course.code}</Badge>}
                </div>
              </Card>
            ))}
          </div>
        )}
      </section>

      <section className="space-y-3">
        <h2 className="text-sm font-semibold text-gray-900">Quiz status</h2>
        {courseQuizzes.length > 0 ? (
          <div className="space-y-3">
            {courseQuizzes.map((entry, index) => {
              const quizzes = entry.quizzes ?? entry.quizStatus ?? []
              return (
                <Card key={String(entry.id ?? index)}>
                  <div className="flex items-center gap-2">
                    <CourseLink course={entry} />
                    {entry.code && <Badge>{entry.code}</Badge>}
                  </div>
                  {quizzes.length === 0 ? (
                    <p className="mt-2 text-sm text-gray-500">No quizzes yet.</p>
                  ) : (
                    <ul className="mt-2 divide-y divide-gray-50">
                      {quizzes.map((quiz, quizIndex) => {
                        const id = quizId(quiz)
                        const attempted = isAttempted(quiz)
                        return (
                          <li key={String(id ?? quizIndex)} className="flex items-center justify-between gap-2 py-2 text-sm">
                            <span className="text-gray-800">{quizTitle(quiz)}</span>
                            <span className="flex items-center gap-2">
                              {typeof quiz.bestScore === 'number' ? (
                                <span className="font-medium text-gray-900">{quiz.bestScore}</span>
                              ) : typeof quiz.score === 'number' ? (
                                <span className="font-medium text-gray-900">{quiz.score}</span>
                              ) : null}
                              {attempted ? (
                                <Badge color="green">Attempted</Badge>
                              ) : (
                                <Badge color="amber">Not attempted</Badge>
                              )}
                              {id !== null && (
                                <Link
                                  to={`/quizzes/${id}`}
                                  className="text-xs font-medium text-indigo-600 hover:text-indigo-500"
                                >
                                  Open
                                </Link>
                              )}
                            </span>
                          </li>
                        )
                      })}
                    </ul>
                  )}
                </Card>
              )
            })}
          </div>
        ) : quizStatus.length === 0 ? (
          <EmptyState title="No quizzes yet" description="Quizzes appear here once your courses publish them." />
        ) : (
          <Card>
            <ul className="divide-y divide-gray-50">
              {quizStatus.map((quiz, index) => {
                const id = quizId(quiz)
                const attempted = isAttempted(quiz)
                return (
                  <li key={String(id ?? index)} className="flex items-center justify-between gap-2 py-2 text-sm">
                    <span className="text-gray-800">{quizTitle(quiz)}</span>
                    <span className="flex items-center gap-2">
                      {typeof quiz.bestScore === 'number' ? (
                        <span className="font-medium text-gray-900">{quiz.bestScore}</span>
                      ) : typeof quiz.score === 'number' ? (
                        <span className="font-medium text-gray-900">{quiz.score}</span>
                      ) : null}
                      {attempted ? <Badge color="green">Attempted</Badge> : <Badge color="amber">Not attempted</Badge>}
                      {id !== null && (
                        <Link to={`/quizzes/${id}`} className="text-xs font-medium text-indigo-600 hover:text-indigo-500">
                          Open
                        </Link>
                      )}
                    </span>
                  </li>
                )
              })}
            </ul>
          </Card>
        )}
      </section>
    </div>
  )
}

function InstructorDashboard() {
  const query = useInstructorDashboard()

  if (query.isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-1/4" />
        <Skeleton className="h-32 w-full" />
        <Skeleton className="h-32 w-full" />
      </div>
    )
  }
  if (query.isError) return <ErrorState onRetry={() => void query.refetch()} />

  const data = query.data ?? {}
  const courses = data.courses ?? []
  const announcements = data.announcements ?? []

  return (
    <div className="space-y-6">
      <PageHeader title="Dashboard" description="Your courses, quiz results, and announcements." />

      <section className="space-y-3">
        <h2 className="text-sm font-semibold text-gray-900">Your courses</h2>
        {courses.length === 0 ? (
          <EmptyState title="No courses yet" description="Courses you are assigned to appear here." />
        ) : (
          <div className="grid gap-3 sm:grid-cols-2">
            {courses.map((course, index) => {
              const summaries = course.quizResults ?? course.quizResultSummaries ?? []
              return (
                <Card key={String(course.id ?? index)}>
                  <div className="flex items-center justify-between gap-2">
                    <CourseLink course={course} />
                    {course.code && <Badge>{course.code}</Badge>}
                  </div>
                  {summaries.length > 0 && (
                    <p className="mt-2 text-xs text-gray-500">
                      {summaries.length} quiz result summar{summaries.length === 1 ? 'y' : 'ies'}
                    </p>
                  )}
                  <div className="mt-3 flex gap-3 text-xs font-medium">
                    {course.id !== undefined && course.id !== null && (
                      <>
                        <Link to={`/courses/${course.id}/grades`} className="text-indigo-600 hover:text-indigo-500">
                          Grades
                        </Link>
                        <Link to={`/courses/${course.id}/quizzes`} className="text-indigo-600 hover:text-indigo-500">
                          Quizzes
                        </Link>
                      </>
                    )}
                  </div>
                </Card>
              )
            })}
          </div>
        )}
      </section>

      <section className="space-y-3">
        <h2 className="text-sm font-semibold text-gray-900">Your announcements</h2>
        {announcements.length === 0 ? (
          <EmptyState title="No announcements yet" description="Publish one from a course page." />
        ) : (
          <Card>
            <ul className="divide-y divide-gray-50">
              {announcements.map((item, index) => (
                <li key={String(item.id ?? index)} className="flex items-center justify-between gap-2 py-2 text-sm">
                  <span className="text-gray-800">{item.title ?? 'Announcement'}</span>
                  {item.createdAt && <span className="text-xs text-gray-400">{item.createdAt.slice(0, 10)}</span>}
                </li>
              ))}
            </ul>
          </Card>
        )}
      </section>
    </div>
  )
}

function AdminDashboard() {
  const query = useAdminDashboard()

  if (query.isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-1/4" />
        <Skeleton className="h-28 w-full" />
      </div>
    )
  }
  if (query.isError) return <ErrorState onRetry={() => void query.refetch()} />

  const data = query.data ?? {}
  const counts: Array<{ label: string; value: number | undefined }> = [
    { label: 'Users', value: data.userCount ?? data.users },
    { label: 'Courses', value: data.courseCount ?? data.courses },
    { label: 'Enrollments', value: data.enrollmentCount ?? data.enrollments },
  ]

  return (
    <div className="space-y-6">
      <PageHeader title="Dashboard" description="Overall system counts." />

      {counts.every((c) => c.value === undefined) ? (
        <EmptyState title="No counts available" />
      ) : (
        <div className="grid gap-3 sm:grid-cols-3">
          {counts.map((count) => (
            <Card key={count.label}>
              <p className="text-xs font-medium uppercase tracking-wide text-gray-500">{count.label}</p>
              <p className="mt-1 text-2xl font-semibold text-gray-900">
                {count.value !== undefined ? count.value : '—'}
              </p>
            </Card>
          ))}
        </div>
      )}

      <div className="flex gap-3">
        <Link to="/admin/users">
          <Button variant="secondary">Manage users</Button>
        </Link>
        <Link to="/admin/courses">
          <Button variant="secondary">Manage courses</Button>
        </Link>
      </div>
    </div>
  )
}

/** Route element — picks the role-specific endpoint from the session's user (§7.10.1). */
export function DashboardScreen() {
  const { user } = useAuth()

  if (!user) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-1/4" />
        <Skeleton className="h-32 w-full" />
      </div>
    )
  }

  if (user.role === 'STUDENT') return <StudentDashboard />
  if (user.role === 'INSTRUCTOR') return <InstructorDashboard />
  return <AdminDashboard />
}
