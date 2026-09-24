import { useState } from 'react'
import type { FormEvent } from 'react'
import { useSearchParams } from 'react-router'
import { isApiError } from '../../api/errors'
import type { Role, User } from '../../auth/types'
import { Badge, Card, PageHeader } from '../../components/Layout'
import { Button } from '../../components/Button'
import { Input, Select } from '../../components/FormField'
import { ConfirmDialog, Modal } from '../../components/Modal'
import { EmptyState, ErrorState } from '../../components/States'
import { Pagination } from '../../components/Pagination'
import { Skeleton } from '../../components/States'
import { useCreateUser, useDeactivateUser, useUpdateUser, useUsers } from './queries'

const ROLES: Role[] = ['STUDENT', 'INSTRUCTOR', 'ADMIN']

interface UserFormErrors {
  fullName?: string
  email?: string
  password?: string
  role?: string
  form?: string
}

function UserForm({
  mode,
  initial,
  onSubmit,
  submitting,
  errors,
}: {
  mode: 'create' | 'edit'
  initial?: User
  onSubmit: (values: { fullName: string; email: string; password: string; role: Role }) => void
  submitting: boolean
  errors: UserFormErrors
}) {
  const [fullName, setFullName] = useState(initial?.fullName ?? '')
  const [email, setEmail] = useState(initial?.email ?? '')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<Role>(initial?.role ?? 'STUDENT')

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    onSubmit({ fullName, email, password, role })
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4" noValidate>
      {errors.form && (
        <div className="rounded-md bg-red-50 px-3 py-2 text-sm font-medium text-red-700" role="alert">
          {errors.form}
        </div>
      )}
      <Input
        label="Full name"
        name="fullName"
        value={fullName}
        onChange={(e) => setFullName(e.target.value)}
        error={errors.fullName}
        required
      />
      <Input
        label="Email"
        type="email"
        name="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        error={errors.email}
        disabled={mode === 'edit'}
        hint={mode === 'edit' ? 'Email cannot be changed here.' : undefined}
        required={mode === 'create'}
      />
      {mode === 'create' && (
        <Input
          label="Password"
          type="password"
          name="password"
          autoComplete="new-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          error={errors.password}
          required
        />
      )}
      <Select label="Role" name="role" value={role} onChange={(e) => setRole(e.target.value as Role)} error={errors.role}>
        {ROLES.map((r) => (
          <option key={r} value={r}>
            {r}
          </option>
        ))}
      </Select>
      <div className="flex justify-end gap-3 pt-2">
        <Button type="submit" loading={submitting}>
          {mode === 'create' ? 'Create user' : 'Save changes'}
        </Button>
      </div>
    </form>
  )
}

export function AdminUsersScreen() {
  const [searchParams, setSearchParams] = useSearchParams()

  const roleParam = (searchParams.get('role') ?? '') as Role | ''
  const page = Number(searchParams.get('page') ?? '0') || 0

  const filters = { role: roleParam, page, size: 20 }
  const usersQuery = useUsers(filters)

  const createUser = useCreateUser()
  const updateUser = useUpdateUser()
  const deactivateUser = useDeactivateUser()

  const [createOpen, setCreateOpen] = useState(false)
  const [editing, setEditing] = useState<User | null>(null)
  const [deactivating, setDeactivating] = useState<User | null>(null)
  const [createErrors, setCreateErrors] = useState<UserFormErrors>({})
  const [editErrors, setEditErrors] = useState<UserFormErrors>({})

  const setParam = (key: string, value: string | null) => {
    const next = new URLSearchParams(searchParams)
    if (value === null || value === '') next.delete(key)
    else next.set(key, value)
    if (key !== 'page') next.delete('page')
    setSearchParams(next)
  }

  const onCreate = (values: { fullName: string; email: string; password: string; role: Role }) => {
    setCreateErrors({})
    createUser.mutate(values, {
      onSuccess: () => setCreateOpen(false),
      onError: (error) => {
        if (isApiError(error) && error.status === 409) {
          setCreateErrors({ email: 'An account with this email already exists.' })
        } else if (isApiError(error) && error.fieldErrors) {
          setCreateErrors(error.fieldErrors)
        } else if (isApiError(error)) {
          setCreateErrors({ form: error.message })
        } else {
          setCreateErrors({ form: 'Something went wrong.' })
        }
      },
    })
  }

  const onEdit = (values: { fullName: string; email: string; password: string; role: Role }) => {
    if (!editing) return
    setEditErrors({})
    updateUser.mutate(
      { id: editing.id, input: { fullName: values.fullName, role: values.role } },
      {
        onSuccess: () => setEditing(null),
        onError: (error) => {
          if (isApiError(error) && error.fieldErrors) setEditErrors(error.fieldErrors)
          else if (isApiError(error)) setEditErrors({ form: error.message })
          else setEditErrors({ form: 'Something went wrong.' })
        },
      },
    )
  }

  const onDeactivate = () => {
    if (!deactivating) return
    deactivateUser.mutate(deactivating.id, {
      onSuccess: () => setDeactivating(null),
    })
  }

  const data = usersQuery.data

  return (
    <div className="space-y-6">
      <PageHeader
        title="Users"
        description="Create instructor and admin accounts, promote students, deactivate users."
        actions={<Button onClick={() => setCreateOpen(true)}>New user</Button>}
      />

      <Card className="!p-4">
        <div className="flex items-end gap-3">
          <div className="w-48">
            <Select
              label="Role"
              name="roleFilter"
              value={roleParam}
              onChange={(e) => setParam('role', e.target.value || null)}
            >
              <option value="">All roles</option>
              {ROLES.map((r) => (
                <option key={r} value={r}>
                  {r}
                </option>
              ))}
            </Select>
          </div>
        </div>
      </Card>

      <Card className="!p-0">
        {usersQuery.isLoading ? (
          <div className="space-y-3 p-6">
            {Array.from({ length: 5 }, (_, i) => (
              <Skeleton key={i} className="h-10 w-full" />
            ))}
          </div>
        ) : usersQuery.isError ? (
          <div className="p-6">
            <ErrorState
              message={isApiError(usersQuery.error) && usersQuery.error.status === 403 ? 'You do not have access to user management.' : 'Failed to load users.'}
              onRetry={() => void usersQuery.refetch()}
            />
          </div>
        ) : !data || data.content.length === 0 ? (
          <div className="p-6">
            <EmptyState title="No users found" description="Try a different role filter or create a user." />
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50 text-left text-xs font-medium uppercase tracking-wide text-gray-500">
                <tr>
                  <th className="px-4 py-3">Name</th>
                  <th className="px-4 py-3">Email</th>
                  <th className="px-4 py-3">Role</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 bg-white">
                {data.content.map((user) => (
                  <tr key={String(user.id)} className={user.isActive === false ? 'opacity-60' : undefined}>
                    <td className="px-4 py-3 font-medium text-gray-900">{user.fullName}</td>
                    <td className="px-4 py-3 text-gray-600">{user.email}</td>
                    <td className="px-4 py-3">
                      <Badge color={user.role === 'ADMIN' ? 'red' : user.role === 'INSTRUCTOR' ? 'indigo' : 'gray'}>
                        {user.role}
                      </Badge>
                    </td>
                    <td className="px-4 py-3">
                      {user.isActive === false ? <Badge color="red">Inactive</Badge> : <Badge color="green">Active</Badge>}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <div className="flex justify-end gap-2">
                        <Button size="sm" variant="secondary" onClick={() => setEditing(user)}>
                          Edit
                        </Button>
                        {user.isActive !== false && (
                          <Button size="sm" variant="danger" onClick={() => setDeactivating(user)}>
                            Deactivate
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="px-4 pb-4">
              <Pagination
                page={data.number}
                totalPages={data.totalPages}
                totalElements={data.totalElements}
                onPageChange={(next) => setParam('page', String(next))}
              />
            </div>
          </div>
        )}
      </Card>

      <Modal open={createOpen} title="Create user" onClose={() => setCreateOpen(false)}>
        <UserForm mode="create" onSubmit={onCreate} submitting={createUser.isPending} errors={createErrors} />
      </Modal>

      <Modal open={editing !== null} title="Edit user" onClose={() => setEditing(null)}>
        {editing && (
          <UserForm
            mode="edit"
            initial={editing}
            onSubmit={onEdit}
            submitting={updateUser.isPending}
            errors={editErrors}
          />
        )}
      </Modal>

      <ConfirmDialog
        open={deactivating !== null}
        title="Deactivate user"
        message={`Deactivate ${deactivating?.fullName ?? ''}? They will no longer be able to sign in.`}
        confirmLabel="Deactivate"
        destructive
        loading={deactivateUser.isPending}
        onConfirm={onDeactivate}
        onCancel={() => setDeactivating(null)}
      />
    </div>
  )
}
