# Phase 1 — Entities & repositories (one commit per bounded group)

Status legend: `[x]` done, `[ ]` not yet.

- [x] **1.1 User entity + Role enum + UserRepository**
  Fields per spec §2. `email` unique index (`@Column(unique=true)`), `role`/`isActive` indexed per §2a.
  **Acceptance:** `@DataJpaTest` (H2 in-memory, test-only profile) saves a `User` and finds it by email; schema has a unique constraint on `email`.
  **Commit:** `feat(user): add User entity, Role enum, repository`

- [x] **1.2 Course + Enrollment entities/repos**
  `Course.instructor` FK indexed; `Course.code` unique; `Enrollment` composite unique `(student_id, course_id)`; `Enrollment.course` gets its own index per §2a rationale.
  **Acceptance:** `@DataJpaTest` — saving a duplicate `(student, course)` enrollment throws `DataIntegrityViolationException`; saving a second course with an already-used `code` (any `term`) throws `DataIntegrityViolationException`.
  **Commit:** `feat(course): add Course and Enrollment entities, repositories`

- [x] **1.3 Section + MarkdownContent entities/repos**
  Composite index `(course_id, orderIndex)` on `Section`.
  **Acceptance:** `@DataJpaTest` — sections for a course come back ordered by `orderIndex` when queried with `findByCourseOrderByOrderIndexAsc`.
  **Commit:** `feat(section): add Section and MarkdownContent entities, repositories`

- [x] **1.4 Quiz + Question + QuestionOption entities/repos**
  Composite `(course_id, published)` on `Quiz`.
  **Acceptance:** `@DataJpaTest` — a `Quiz` with 2 `Question`s, each with options, persists and reloads via cascade with the full tree intact.
  **Commit:** `feat(quiz): add Quiz, Question, QuestionOption entities, repositories`

- [x] **1.5 QuizAttempt + QuizAnswer entities/repos**
  Composite unique `(quiz_id, student_id)` on `QuizAttempt` — this is the single-attempt guard, not just an index.
  **Acceptance:** `@DataJpaTest` — saving a second `QuizAttempt` for the same `(quiz, student)` throws `DataIntegrityViolationException`.
  **Commit:** `feat(quiz): add QuizAttempt and QuizAnswer entities, repositories`

- [x] **1.6 DiscussionPost + Announcement entities/repos**
  Composite `(course_id, createdAt)` on both; `parentPost` self-FK on `DiscussionPost` indexed.
  **Acceptance:** `@DataJpaTest` — a reply saved with a non-null `parentPost` loads with its parent; querying by `course` + `createdAt DESC` paginates correctly.
  **Commit:** `feat(communication): add DiscussionPost and Announcement entities, repositories`
