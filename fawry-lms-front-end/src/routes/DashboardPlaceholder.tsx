import { Card } from '../components/Layout'

/** Temporary stand-in; replaced by the role-specific dashboards in build step 12 (frontend-build.md §7.10). */
export function DashboardPlaceholder() {
  return (
    <Card>
      <h2 className="text-lg font-semibold text-gray-900">Dashboard</h2>
      <p className="mt-1 text-sm text-gray-500">
        Role-specific dashboard content arrives in build step 12.
      </p>
    </Card>
  )
}
