import { NavLink, Outlet, useNavigate } from 'react-router'
import { useAuth } from '../auth/context'
import type { Role } from '../auth/types'
import { Button } from '../components/Button'

interface NavItem {
  to: string
  label: string
  roles?: Role[]
}

/** Role-filtered per frontend-build.md §6.1; grows as feature steps land. */
const NAV_ITEMS: NavItem[] = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/profile', label: 'Profile' },
  { to: '/admin/users', label: 'Users', roles: ['ADMIN'] },
]

function navItemsForRole(role: Role): NavItem[] {
  return NAV_ITEMS.filter((item) => !item.roles || item.roles.includes(role))
}

export function RootLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  if (!user) return null

  const items = navItemsForRole(user.role)

  const onLogout = async () => {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="min-h-svh bg-gray-50">
      <div className="flex min-h-svh">
        <aside className="flex w-60 shrink-0 flex-col border-r border-gray-200 bg-white">
          <div className="flex h-16 items-center border-b border-gray-200 px-5">
            <span className="text-sm font-semibold tracking-tight text-gray-900">University LMS</span>
          </div>
          <nav className="flex-1 space-y-1 p-3" aria-label="Main">
            {items.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  `block rounded-md px-3 py-2 text-sm font-medium transition-colors ${
                    isActive
                      ? 'bg-indigo-50 text-indigo-700'
                      : 'text-gray-700 hover:bg-gray-100 hover:text-gray-900'
                  }`
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
          <div className="border-t border-gray-200 p-4">
            <div className="flex items-center gap-3">
              {user.profilePictureUrl ? (
                <img
                  src={user.profilePictureUrl}
                  alt=""
                  className="size-9 rounded-full object-cover ring-1 ring-gray-200"
                />
              ) : (
                <div className="flex size-9 items-center justify-center rounded-full bg-indigo-100 text-sm font-semibold text-indigo-700">
                  {user.fullName.charAt(0).toUpperCase()}
                </div>
              )}
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium text-gray-900">{user.fullName}</p>
                <p className="truncate text-xs text-gray-500">{user.role}</p>
              </div>
            </div>
            <Button variant="ghost" size="sm" className="mt-3 w-full justify-center" onClick={onLogout}>
              Logout
            </Button>
          </div>
        </aside>
        <div className="flex min-w-0 flex-1 flex-col">
          <main className="flex-1 p-6 lg:p-8">
            <Outlet />
          </main>
        </div>
      </div>
    </div>
  )
}
