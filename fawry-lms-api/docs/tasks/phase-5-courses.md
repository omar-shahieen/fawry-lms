# Phase 5 — Courses module

Status legend: `[x]` done, `[ ]` not yet.

- [x] **5.1 GET /api/courses, GET /api/courses/{id}**
  Any authenticated user; only `isActive=true` in the list; `?search=` (title, LIKE), `?term=` (exact), `?code=` (exact), combinable with each other and pagination.
  **Acceptance:** integration test — soft-deleted course excluded from list but still fetchable by id; a query combining `?search=&term=&page=` returns the expected filtered/paginated subset; non-existent id → 404.
  **Commit:** `feat(course): implement course list and get-by-id endpoints`

- [x] **5.2 POST /api/courses, PATCH /api/courses/{id}, DELETE /api/courses/{id}**
  Admin-only create/delete; admin-or-owning-instructor update. Instructors cannot create courses under any path.
  **Acceptance:** integration tests — instructor calling create → 403; instructor editing someone else's course → 403; instructor editing their own → 200; creating a course with a `code` that already exists (in any `term`) → 409; delete sets `isActive=false` (row still exists, children not cascade-deleted).
  **Commit:** `feat(course): implement course create, update, and soft-delete endpoints`

- [x] **5.3 PATCH /api/courses/{id}/assign-instructor**
  **Acceptance:** integration test — admin reassigns instructor; body `{instructorId}` must reference a user with role `INSTRUCTOR` or 400.
  **Commit:** `feat(course): implement assign-instructor endpoint`

- [ ] **5.4 POST /api/courses/{id}/enroll, GET /api/courses/{id}/students**
  **Acceptance:** integration tests — student self-enroll twice → second call 409 (unique constraint surfaced as a clean error, not a raw 500); roster visible to admin and the course's own instructor, 403 for other instructors/students. No unenroll endpoint exists — not tested because it's out of scope.
  **Commit:** `feat(course): implement self-enrollment and roster endpoints`
