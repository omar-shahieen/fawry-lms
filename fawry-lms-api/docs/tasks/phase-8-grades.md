# Phase 8 — Grades

Status legend: `[x]` done, `[ ]` not yet.

- [ ] **8.1 GET /api/students/me/grades**
  **Acceptance:** integration test — response grouped by enrolled course, each with that course's quiz results (one score per attempted quiz, since single-attempt means no best-of/averaging logic to verify); a student cannot retrieve another student's grades via this endpoint (there's no id param — scoped to `authentication.principal`).
  **Commit:** `feat(grade): implement student grades view`

- [ ] **8.2 GET /api/courses/{courseId}/grades**
  **Acceptance:** integration test — flat paginated `{student, quiz, score}` rows; instructor sees only their own course (403 on others'); admin sees any course, bypassing ownership.
  **Commit:** `feat(grade): implement instructor/admin course grades view`
