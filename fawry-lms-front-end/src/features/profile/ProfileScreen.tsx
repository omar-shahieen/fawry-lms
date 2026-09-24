import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router'
import { useAuth } from '../../auth/context'
import { isApiError } from '../../api/errors'
import { Badge, Card, PageHeader } from '../../components/Layout'
import { Button } from '../../components/Button'
import { Input } from '../../components/FormField'
import { ErrorState } from '../../components/States'
import { updateProfile } from './api'

export function ProfileScreen() {
  const { user, refreshUser } = useAuth()

  const [fullName, setFullName] = useState(user?.fullName ?? '')
  const [profilePictureUrl, setProfilePictureUrl] = useState(user?.profilePictureUrl ?? '')
  const [errors, setErrors] = useState<{ fullName?: string; profilePictureUrl?: string; form?: string }>({})
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)

  if (!user) return <ErrorState message="Your session could not be loaded." />

  const isStudent = user.role === 'STUDENT'
  const enrolledCourses = isStudent ? (user.enrolledCourses ?? []) : []

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault()
    const nextErrors: typeof errors = {}
    if (!fullName.trim()) nextErrors.fullName = 'Full name is required.'
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) return

    setSaving(true)
    setSaved(false)
    setErrors({})
    try {
      await updateProfile({ fullName: fullName.trim(), profilePictureUrl })
      await refreshUser()
      setSaved(true)
    } catch (error) {
      if (isApiError(error) && error.fieldErrors) {
        setErrors(error.fieldErrors)
      } else if (isApiError(error)) {
        setErrors({ form: error.message })
      } else {
        setErrors({ form: 'Something went wrong. Please try again.' })
      }
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <PageHeader title="Profile" description="Manage your name and profile picture." />

      <Card>
        <form onSubmit={onSubmit} className="space-y-4" noValidate>
          {errors.form && (
            <div className="rounded-md bg-red-50 px-3 py-2 text-sm font-medium text-red-700" role="alert">
              {errors.form}
            </div>
          )}
          {saved && (
            <div className="rounded-md bg-green-50 px-3 py-2 text-sm font-medium text-green-700" role="status">
              Profile updated.
            </div>
          )}

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <span className="block text-sm font-medium text-gray-800">Email</span>
              <p className="rounded-md bg-gray-50 px-3 py-2 text-sm text-gray-600 ring-1 ring-inset ring-gray-200">
                {user.email}
              </p>
            </div>
            <div className="space-y-1.5">
              <span className="block text-sm font-medium text-gray-800">Role</span>
              <p className="rounded-md bg-gray-50 px-3 py-2 text-sm text-gray-600 ring-1 ring-inset ring-gray-200">
                <Badge color="indigo">{user.role}</Badge>
              </p>
            </div>
          </div>

          <Input
            label="Full name"
            name="fullName"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            error={errors.fullName}
            required
          />
          <Input
            label="Profile picture URL"
            name="profilePictureUrl"
            type="url"
            hint="Paste a link to an image — no file upload."
            value={profilePictureUrl}
            onChange={(e) => setProfilePictureUrl(e.target.value)}
            error={errors.profilePictureUrl}
          />

          <div className="flex justify-end">
            <Button type="submit" loading={saving}>
              Save changes
            </Button>
          </div>
        </form>
      </Card>

      {isStudent && (
        <Card>
          <h2 className="text-sm font-semibold text-gray-900">Enrolled courses</h2>
          {enrolledCourses.length === 0 ? (
            <p className="mt-2 text-sm text-gray-500">You are not enrolled in any courses yet.</p>
          ) : (
            <ul className="mt-3 divide-y divide-gray-100">
              {enrolledCourses.map((course, index) => (
                <li key={String(course.id ?? index)} className="flex items-center justify-between py-2">
                  <span className="text-sm text-gray-800">{course.title ?? 'Course'}</span>
                  {course.id !== undefined && course.id !== null && (
                    <Link
                      to={`/courses/${course.id}`}
                      className="text-xs font-medium text-indigo-600 hover:text-indigo-500"
                    >
                      Open
                    </Link>
                  )}
                </li>
              ))}
            </ul>
          )}
        </Card>
      )}
    </div>
  )
}
