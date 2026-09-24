import { Link } from 'react-router'
import { useAuth } from '../../auth/context'
import { Badge, Card, PageHeader } from '../../components/Layout'
import { Button } from '../../components/Button'
import { EmptyState, ErrorState, Skeleton } from '../../components/States'
import { formatDate } from '../../components/formatDate'
import { useAdminDashboard, useInstructorDashboard, useStudentDashboard } from './queries'

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

  const courses = query.data ?? []

  return (
    <div className="space-y-6">
      <PageHeader title="Dashboard" description="Your enrolled courses and quiz status." />

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
        <div className="space-y-3">
          {courses.map((course, index) => {
            const quizzes = course.quizzes ?? []
            return (
              <Card key={String(course.courseId ?? index)}>
                <div className="flex items-center gap-2">
                  {course.courseId !== undefined && course.courseId !== null ? (
                    <Link
                      to={`/courses/${course.courseId}`}
                      className="font-medium text-indigo-600 hover:text-indigo-500"
                    >
                      {course.courseName ?? 'Course'}
                    </Link>
                  ) : (
                    <span className="font-medium text-gray-900">{course.courseName ?? 'Course'}</span>
                  )}
                </div>
                {quizzes.length === 0 ? (
                  <p className="mt-2 text-sm text-gray-500">No quizzes yet.</p>
                ) : (
                  <ul className="mt-2 divide-y divide-gray-50">
                    {quizzes.map((quiz, quizIndex) => (
                      <li
                        key={String(quiz.quizId ?? quizIndex)}
                        className="flex items-center justify-between gap-2 py-2 text-sm"
                      >
                        <span className="text-gray-800">{quiz.quizTitle ?? 'Quiz'}</span>
                        <span className="flex items-center gap-2">
                          {typeof quiz.score === 'number' && (
                            <span className="font-medium text-gray-900">{quiz.score}</span>
                          )}
                          {quiz.attempted ? (
                            <Badge color="green">Attempted</Badge>
                          ) : (
                            <Badge color="amber">Not attempted</Badge>
                          )}
                          {quiz.quizId !== undefined && quiz.quizId !== null && (
                            <Link
                              to={`/quizzes/${quiz.quizId}`}
                              className="text-xs font-medium text-indigo-600 hover:text-indigo-500"
                            >
                              Open
                            </Link>
                          )}
                        </span>
                      </li>
                    ))}
                  </ul>
                )}
              </Card>
            )
          })}
        </div>
      )}
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

  const courses = query.data?.courses ?? []
  const announcements = query.data?.announcements ?? []

  return (
    <div className="space-y-6">
      <PageHeader title="Dashboard" description="Your courses, quiz results, and announcements." />

      <section className="space-y-3">
        <h2 className="text-sm font-semibold text-gray-900">Your courses</h2>
        {courses.length === 0 ? (
          <EmptyState title="No courses yet" description="Courses you are assigned to appear here." />
        ) : (
          <div className="grid gap-3 sm:grid-cols-2">
            {courses.map((course, index) => (
              <Card key={String(course.courseId ?? index)}>
                <div className="flex items-center justify-between gap-2">
                  {course.courseId !== undefined && course.courseId !== null ? (
                    <Link
                      to={`/courses/${course.courseId}`}
                      className="font-medium text-indigo-600 hover:text-indigo-500"
                    >
                      {course.courseName ?? 'Course'}
                    </Link>
                  ) : (
                    <span className="font-medium text-gray-900">{course.courseName ?? 'Course'}</span>
                  )}
                  {typeof course.averageScore === 'number' && (
                    <Badge>{course.averageScore.toFixed(1)} avg</Badge>
                  )}
                </div>
                <p className="mt-2 text-xs text-gray-500">
                  {course.submittedAttemptCount ?? 0} submitted attempt
                  {(course.submittedAttemptCount ?? 0) === 1 ? '' : 's'}
                </p>
                {course.courseId !== undefined && course.courseId !== null && (
                  <div className="mt-3 flex gap-3 text-xs font-medium">
                    <Link
                      to={`/courses/${course.courseId}/grades`}
                      className="text-indigo-600 hover:text-indigo-500"
                    >
                      Grades
                    </Link>
                    <Link
                      to={`/courses/${course.courseId}/quizzes`}
                      className="text-indigo-600 hover:text-indigo-500"
                    >
                      Quizzes
                    </Link>
                  </div>
                )}
              </Card>
            ))}
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
                <li key={String(item.id ?? index)} className="flex items-start justify-between gap-3 py-2 text-sm">
                  <div>
                    <p className="font-medium text-gray-800">{item.title ?? 'Announcement'}</p>
                    {item.body && <p className="mt-0.5 text-xs text-gray-500 line-clamp-2">{item.body}</p>}
                  </div>
                  {item.createdAt && <span className="shrink-0 text-xs text-gray-400">{formatDate(item.createdAt)}</span>}
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
  const roleCounts = Object.entries(data.userCountsByRole ?? {})
  const counts: Array<{ label: string; value: number | undefined }> = [
    ...roleCounts.map(([role, value]) => ({ label: role, value })),
    { label: 'Courses', value: data.totalCourseCount },
    { label: 'Enrollments', value: data.totalEnrollmentCount },
  ]

  return (
    <div className="space-y-6">
      <PageHeader title="Dashboard" description="Overall system counts." />

      {counts.length === 0 ? (
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
