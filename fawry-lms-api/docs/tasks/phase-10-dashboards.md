# Phase 10 — Dashboards

Status legend: `[x]` done, `[ ]` not yet.

- [x] **10.1 Student dashboard**
  `GET /api/students/me/dashboard`. Per the locked scope, this is deliberately narrow: enrolled courses (id/name), and per-course quiz status. Since a student has at most one attempt per quiz (§7), status is a binary attempted/not-attempted, and the "score" is just that one attempt's score — there is no "best score" to compute. Course progress and an announcements feed are explicitly cut from this endpoint.
  **Acceptance:** integration test — payload contains enrolled courses and, per course, each quiz marked attempted/not-attempted with the single score where attempted; a student's payload never contains another student's data; response contains no progress or announcement fields.
  **Commit:** `feat(dashboard): implement student dashboard`

- [x] **10.2 Instructor dashboard**
  **Acceptance:** integration test — includes only the instructor's own courses, a quiz-results summary per course (attempt counts, average score across submitted attempts), and their own announcements; no content/section summary or discussion feed (cut from scope); no data from courses they're not assigned to.
  **Commit:** `feat(dashboard): implement instructor dashboard`

- [ ] **10.3 Admin dashboard**
  **Acceptance:** integration test — total user count by role, total course count, total enrollment count match seeded fixture data exactly.
  **Commit:** `feat(dashboard): implement admin dashboard`
