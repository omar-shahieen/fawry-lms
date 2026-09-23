# Phase 4 — Users module

Status legend: `[x]` done, `[ ]` not yet.

- [x] **4.1 GET /api/users/me**
  Includes `enrolledCourses` when `role == STUDENT`.
  **Acceptance:** integration test as a student → payload includes enrollments; as instructor → field omitted/empty and no N+1 query blow-up (assert query count with a Hibernate statistics check, or just correctness if that's overkill for the deadline).
  **Commit:** `feat(user): implement GET /api/users/me`

- [ ] **4.2 PATCH /api/users/me**
  Only `fullName`/`profilePictureUrl` editable; `role`/`email`/`isActive` ignored even if sent.
  **Acceptance:** integration test — sending a `role` field in the body does not change the caller's role.
  **Commit:** `feat(user): implement PATCH /api/users/me`

- [ ] **4.3 Admin user list/read**
  `GET /api/users` (paginated, `?role=`), `GET /api/users/{id}`.
  **Acceptance:** integration test — non-admin gets 403; admin with `?role=STUDENT` gets only students; pagination metadata present.
  **Commit:** `feat(user): implement admin user list and get-by-id endpoints`

- [ ] **4.4 Admin user create/update/deactivate**
  `POST /api/users`, `PATCH /api/users/{id}`, `PATCH /api/users/{id}/deactivate`. This is also the only way `INSTRUCTOR` and additional `ADMIN` accounts get created (alongside `PATCH .../{id}` for promoting an existing user).
  **Acceptance:** integration tests — create seeds a bcrypt-hashed password; create with no `profilePictureUrl` supplied gets one auto-generated (same generator as signup); `PATCH .../{id}` can change `role` (e.g. promote Student → Instructor), unlike `PATCH /users/me`; deactivate flips `isActive=false` and a deactivated user's next login attempt returns 401; non-admin caller on any of these three → 403.
  **Commit:** `feat(user): implement admin user create, update, and deactivate endpoints`
