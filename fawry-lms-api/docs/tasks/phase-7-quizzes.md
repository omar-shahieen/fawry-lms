# Phase 7 — Quizzes

Status legend: `[x]` done, `[ ]` not yet.

- [x] **7.1 Quiz CRUD (metadata only) + published toggle**
  **Acceptance:** integration test — student list only sees `published=true`; instructor/admin see both; non-owning instructor editing/publishing → 403.
  **Commit:** `feat(quiz): implement quiz CRUD and published toggle`

- [x] **7.2 Questions/options CRUD**
  `POST .../questions` (2+ options, exactly one `isCorrect`), `PATCH/DELETE /api/questions/{id}`.
  **Acceptance:** integration tests — zero options or no option marked correct → validation error (400/422); more than one `isCorrect` on a question → rejected; instructor/admin detail view includes `isCorrect` per option.
  **Commit:** `feat(quiz): implement question and option management`

- [ ] **7.3 GET /api/quizzes/{id} — student view + attempt lifecycle**
  This is the endpoint with the side effect the spec calls out explicitly (§3 Quiz Attempt Flow): for a `STUDENT`, enrollment-checked. First call for a given `(quiz, student)` creates a `QuizAttempt` with `startedAt = now()` and returns a computed `expiresAt`/`secondsRemaining`. A later call on the same unsubmitted attempt returns the same `startedAt`/`expiresAt` — it does not reset the clock. A call after submission returns the quiz **read-only** with the student's own answers and score, and does not create a second attempt. At no point (pre-submission) does the response expose which option is correct.
  **Acceptance:** integration tests — first GET creates exactly one `QuizAttempt` row and returns `expiresAt`; a second GET before submitting returns the identical `startedAt`/`expiresAt` and still only one `QuizAttempt` row exists; `isCorrect` absent from the student's payload before submission; GET after submission returns the stored answers/score and still only one row exists; instructor/admin GET on the same endpoint (if reused) does not trigger attempt creation.
  **Commit:** `feat(quiz): implement student quiz view with attempt-start side effect`

- [ ] **7.4 POST /api/quizzes/{id}/submit (autograde)**
  Requires an existing attempt (created by 7.3) for `(quiz, student)`. Rejects if no attempt exists (400), if already submitted (409), or if past `startedAt + durationMinutes` per the **server clock** (client-reported time is never trusted). Otherwise grades immediately and sets `submittedAt`.
  **Acceptance:** integration tests — on-time submission returns correct score + per-question correctness and correct answers; submission after the time limit → rejected with a clear error, based on a manipulated `startedAt` in the test fixture rather than trusting any client-sent timestamp; re-submitting an already-submitted attempt → 409; two near-simultaneous submit calls for the same `(quiz, student)` (fired concurrently in the test) result in exactly one persisted, graded attempt and the loser gets 409 — proving the DB unique constraint from 1.5 is the actual guard, not just app-layer logic; submit on an unpublished quiz or for a course the student isn't enrolled in → rejected.
  **Commit:** `feat(quiz): implement quiz submission with autograding and time-limit enforcement`

- [ ] **7.5 GET attempts/me, GET attempts (staff)**
  **Acceptance:** integration test — student sees only their own attempt via `/attempts/me`; a student cannot fetch another student's attempt by any route (403, since there's no id param to swap — assert the query is scoped to `authentication.principal`); staff view (`/attempts`) is paginated and scoped to the instructor's own course unless caller is admin.
  **Commit:** `feat(quiz): implement attempt history endpoints`
