import { Link } from 'react-router'

export function HomePage() {
  return (
    <div className="flex min-h-svh items-center justify-center bg-gray-50">
      <Link
        to="/dashboard"
        className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-500"
      >
        Continue to dashboard
      </Link>
    </div>
  )
}

export function ForbiddenPage() {
  return (
    <div className="flex min-h-svh flex-col items-center justify-center gap-3 bg-gray-50 p-6 text-center">
      <p className="text-sm font-semibold text-indigo-600">403</p>
      <h1 className="text-2xl font-semibold text-gray-900">You don’t have access to this page</h1>
      <p className="max-w-md text-sm text-gray-500">
        Your account doesn’t have permission to view this area. If you think this is a mistake, contact
        your administrator.
      </p>
      <Link to="/dashboard" className="mt-2 text-sm font-medium text-indigo-600 hover:text-indigo-500">
        Back to dashboard
      </Link>
    </div>
  )
}

export function NotFoundPage() {
  return (
    <div className="flex min-h-svh flex-col items-center justify-center gap-3 bg-gray-50 p-6 text-center">
      <p className="text-sm font-semibold text-indigo-600">404</p>
      <h1 className="text-2xl font-semibold text-gray-900">Page not found</h1>
      <p className="max-w-md text-sm text-gray-500">The page you’re looking for doesn’t exist.</p>
      <Link to="/dashboard" className="mt-2 text-sm font-medium text-indigo-600 hover:text-indigo-500">
        Back to dashboard
      </Link>
    </div>
  )
}
