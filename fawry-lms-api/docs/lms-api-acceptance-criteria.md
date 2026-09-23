    # API MVP Acceptance Criteria (Adjusted to Locked Scope)

    The LMS API MVP is accepted when all checks below pass.

    ## 1. Authentication & Authorization

    * `POST /api/auth/signup` with valid `fullName`/`email`/`password` creates a new user and returns `201` with an access + refresh token.
    * A `signup` request that includes a `role` field is ignored — the created user is always `STUDENT`, regardless of what's sent.
    * `POST /api/auth/signup` with a duplicate email returns `409`.
    * `POST /api/auth/signup` with invalid/missing fields returns `400`/`422`.
    * `POST /api/auth/login` with valid credentials returns `200` and an access + refresh token.
    * `POST /api/auth/login` with invalid credentials returns `401`.
    * A deactivated (`isActive=false`) user cannot log in, even with correct credentials (`401`).
    * Protected endpoints without authentication return `401`.
    * A Student requesting an Admin-only endpoint returns `403`.
    * An Instructor requesting another Instructor's restricted resource returns `403`.
    * An authenticated user can retrieve their own profile (`GET /api/users/me`).
    * `POST /api/auth/refresh` exchanges a valid refresh token for a new access token.

    ## 2. User Management

    * A newly signed-up or Admin-created user has a non-null `profilePictureUrl`, auto-generated if not supplied.
    * Admin can create a Student directly (`POST /api/users`).
    * Admin can create an Instructor — **this, along with `PATCH /api/users/{id}`'s role field, is the only way an Instructor account is created**; there is no self-service instructor signup.
    * Admin can create another Admin.
    * Admin can retrieve a paginated list of users.
    * Admin can filter the user list by role (`?role=`).
    * Admin can retrieve a specific user by ID.
    * Admin can update an existing user, including changing their role (e.g. promoting a Student to Instructor).
    * Admin can deactivate a user (soft delete via `isActive=false`); a deactivated user can no longer log in.
    * A non-admin user can update their own name/profile picture, but not their role or active status.
    * Non-admin users cannot create or modify other users through admin endpoints.
    * Creating a user with an invalid email/missing required field returns `400`/`422`.
    * Creating a user with a duplicate email returns `409` (or the project's chosen conflict response).

    ## 3. Course Management

    * Admin can create a course.
    * Creating a course with a `code` that already exists (any term) returns `409` — course codes are globally unique.
    * Admin can assign an Instructor to a course.
    * **Instructors cannot create courses** — course creation is Admin-only.
    * Authorized (authenticated) users can retrieve a course by ID.
    * Authorized users can retrieve the paginated course list.
    * The course list supports `?search=` (matches title), `?term=` (exact), and `?code=` (exact), combinable with each other and with pagination.
    * Instructor can update a course they are assigned to.
    * An Instructor cannot update a course assigned to a different Instructor (`403`).
    * Admin can update/soft-delete any course.
    * Requesting a non-existent course returns `404`.
    * Soft-deleting a course hides it from listings but does not cascade-delete its sections/content/quizzes.

    ## 4. Course Enrollment

    * Student can self-enroll in a course (`POST /api/courses/{id}/enroll`).
    * Student can retrieve their list of enrolled courses.
    * Student cannot access sections/content/quizzes/discussion/announcements of a course they are not enrolled in (`403`/`404`).
    * Attempting to self-enroll in the same course twice returns a conflict response (`409`).
    * **No unenroll/removal endpoint is required** — enrollment is enroll-only for this MVP.

    ## 5. Course Sections / Modules

    * Instructor can create a section within a course they are assigned to.
    * Instructor can update a section within their own course.
    * Instructor can delete a section within their own course.
    * Enrolled Student can retrieve the ordered sections of a course (ordered by `orderIndex`).
    * A section cannot be created referencing a course the requesting Instructor doesn't own.
    * Unauthorized users (unenrolled students, other instructors) cannot create, update, or delete sections.
    * Section ordering is persisted and returned consistently by the API.

    ## 6. Course Content (Markdown only)

    * Instructor can create Markdown content inside a section of their own course.
    * Instructor can update Markdown content they own.
    * Instructor can delete Markdown content they own.
    * Enrolled Student can retrieve the Markdown content of a course they're enrolled in.
    * Student cannot create, modify, or delete Markdown content.
    * Markdown content belonging to a course the requester has no access to returns `403`/`404`.
    * **PDF material is out of scope for this MVP** — no PDF upload/retrieval endpoints are required.

    ## 7. Quizzes

    ### Quiz Management

    * Instructor can create a quiz for their own course, including setting `durationMinutes`.
    * Instructor can update the quiz.
    * Instructor can publish/unpublish the quiz.
    * Instructor can delete a quiz.
    * Enrolled Student can retrieve only **published** quizzes for their course.
    * Student cannot retrieve unpublished quizzes.

    ### Questions

    * Instructor can add a multiple-choice question to their quiz, with 2 or more options.
    * Instructor can mark exactly one option per question as correct.
    * Invalid questions (e.g. zero options, no correct option marked) are rejected with a validation error.
    * When a Student fetches quiz questions, the correct-option flag is not exposed before submission.

    ### Quiz Attempts (single attempt, timer starts on first view)

    * A Student's **first** `GET /api/quizzes/{id}` on a published quiz they're enrolled in creates a `QuizAttempt` and starts the clock (`startedAt = now()`); the response includes a computed `expiresAt`/`secondsRemaining`.
    * A **subsequent** `GET` on the same unsubmitted quiz does **not** reset `startedAt` — the original deadline holds regardless of how many times the student re-fetches it.
    * Enrolled Student can submit answers for a published quiz within the time limit (`POST /api/quizzes/{id}/submit`).
    * A submission after `startedAt + durationMinutes` has elapsed is rejected, based on server time — not client-reported time.
    * API auto-grades an on-time submission immediately and returns the score plus per-question correct/incorrect + correct answer.
    * **Each student may submit at most one attempt per quiz.** A second `POST /api/quizzes/{id}/submit` for a quiz already submitted by that student returns `409`.
    * `QuizAttempt(quiz_id, student_id)` is DB-enforced unique — a race between two near-simultaneous submit calls for the same student/quiz still results in exactly one persisted attempt, with the loser receiving `409`.
    * After submission, `GET /api/quizzes/{id}` returns the quiz **read-only**, including the student's own answers and score (their permanent review view) — it does not create a second attempt.
    * Student can retrieve their own attempt (`GET /api/quizzes/{id}/attempts/me`).
    * Student cannot retrieve another Student's quiz attempt (`403`).
    * Instructor can retrieve all students' attempts/results for a quiz in their own course.
    * Student cannot submit an attempt for a quiz in a course they are not enrolled in.
    * Student cannot submit an attempt for an unpublished quiz.

    ## 8. Grades

    * After a quiz attempt is graded, it's reflected in the Student's grade data for that course.
    * Student can retrieve their quiz results for a specific enrolled course.
    * Student can retrieve their quiz results grouped across **all** enrolled courses (`GET /api/students/me/grades`).
    * Student cannot retrieve another Student's grades.
    * Instructor can retrieve a flat `{student, quiz, score}` list for all students in their own course.
    * Admin can retrieve grade information for any course (Admin bypasses ownership checks).
    * **No single aggregated "course grade" is computed** — grade data is exposed as a list of quiz results, not a combined average/total.
    * Since each student has at most one attempt per quiz (§7), a student's "grade" on a quiz is simply that attempt's score — there is no best-of-multiple-attempts logic to verify.

    ## 9. Announcements

    * Instructor can create an announcement for their own course.
    * Instructor can update their own announcement.
    * Instructor can delete their own announcement.
    * Admin can create/update/delete an announcement for any course.
    * Enrolled Student can retrieve a course's announcements.
    * Student cannot create or modify announcements.
    * Student cannot retrieve announcements from a course they can't access.
    * Requesting a non-existent announcement returns `404`.

    ## 10. Discussion Board

    * Enrolled Student can create a top-level discussion post in their course.
    * Instructor can create a discussion post in their own course.
    * **Admin can also create a discussion post** in any course (not moderation-only).
    * Authorized course members (enrolled students, the assigned instructor, Admin) can retrieve discussion posts and their replies.
    * Authorized course members can reply to a top-level post.
    * **Replying to a reply is rejected** — the API enforces exactly one level of nesting (post → replies, no reply-to-reply).
    * Student cannot access discussions for courses they are not enrolled in.
    * A user can edit/delete their own post/reply; a non-owner (non-Admin) cannot.
    * Admin can delete any post/reply (moderation), even if not the author.
    * Requesting a non-existent discussion post returns `404`.

    ## 11. Student Dashboard API

    `GET /api/students/me/dashboard` must return enough data to construct the Student dashboard.

    For a Student with enrolled courses and quiz activity, the response must contain:

    * Enrolled courses (id/name).
    * Per-course quiz status (attempted / not attempted — a Student either has a `QuizAttempt` for a given quiz or does not, since only one is ever possible).
    * Per-course quiz scores (the student's single attempt score per quiz, not a "best of" — see §7/§8).

    The endpoint must not return another Student's private data.

    > Note: **course progress** and an **announcements feed** are not part of this MVP's dashboard — they were explicitly cut from scope.

    ## 12. Instructor Dashboard API

    `GET /api/instructors/me/dashboard` must return enough data to construct the Instructor dashboard.

    For an Instructor with assigned courses, the response must contain:

    * Their assigned courses (id/name).
    * A quiz results summary per course (e.g. attempt counts, average score across submitted attempts).
    * Their own announcements.

    The endpoint must not return data belonging to courses the Instructor isn't assigned to.

    > Note: a **content/section summary** and a **discussion activity feed** are not part of this MVP's dashboard — they were explicitly cut from scope.

    ## 13. Admin Dashboard & Admin API

    * Admin dashboard (`GET /api/admin/dashboard`) returns total user count (by role), total course count, and total enrollment count.
    * Admin can retrieve/manage users (Section 2).
    * Admin can retrieve/create/update/soft-delete courses (Section 3).
    * Admin can assign instructors to courses.
    * Non-admin users receive `403` when accessing admin-only endpoints.

    ## 14. Validation & Error Handling

    For every applicable endpoint:

    * Missing required fields return `400`/`422`.
    * Invalid ID/path parameters return a validation error or `404`.
    * Invalid enum values (e.g. an unrecognized role) are rejected.
    * Invalid foreign-key references (e.g. assigning a non-existent instructor) are rejected.
    * Unauthorized requests return `401`.
    * Authenticated users without sufficient permissions return `403`.
    * Non-existent resources return `404`.
    * Duplicate resources violating a uniqueness constraint (user email, course code, enrollment pair, quiz attempt pair) return `409`.
    * Unexpected server errors do not expose stack traces or internal details — all errors follow the standard error response shape defined in the implementation spec.

    ## 15. API Documentation

    * Every MVP endpoint appears in Swagger UI (via springdoc-openapi), including `POST /api/auth/signup`.
    * Each endpoint documents its HTTP method, path, and required role/auth.
    * Request parameters and body schemas are documented.
    * Success and error response schemas are documented.
    * A developer can execute the complete MVP workflow using the Swagger docs alone.

    ## 16. Automated Test Coverage

    Per the implementation spec (§7.5), automated test coverage for this MVP is intentionally minimal and scoped to the two areas with the most non-obvious correctness rules, not exhaustive CRUD coverage:

    * **Authorization matrix:** at least one test per role for each restricted-endpoint pattern — admin-only, own-or-admin, enrolled-or-staff — covering both the allowed and denied path.
    * **Quiz-attempt state machine:** first `GET` creates an attempt and starts the timer; a repeated `GET` does not reset `startedAt`; a submit after the deadline is rejected; a second submit for an already-submitted quiz is rejected (`409`).

    Broader CRUD happy-path tests across other modules (users, courses, sections, content, announcements, discussion) are lower priority and may be omitted if time is constrained — acceptance for this MVP does not require full coverage of those paths, only that the endpoints themselves behave correctly per the checks in Sections 1–14.

    ## 17. Deployment & Seed Data

    * `docker-compose up` from a clean checkout brings up both Postgres and the API with no manual setup steps.
    * On first startup against an empty database, seed data is created automatically: 1 Admin, 2–3 Instructors, 5–8 Students, 2–3 courses with sections/content, at least one published quiz per course with questions, a few pre-seeded quiz attempts, and sample announcements/discussion posts (including one reply).
    * Seeded account credentials (all roles) are documented in the README.
    * Re-running `docker-compose up` against an already-seeded database does not duplicate seed data.

    ## 18. End-to-End Acceptance Test

    A clean test environment must support the following sequence without direct database manipulation:

    ```text
    1. docker-compose up — Admin account exists via seed data
    2. New user self-registers via POST /api/auth/signup — account is created as STUDENT regardless of any role field sent, tokens returned
    3. Admin creates an Instructor
    4. Admin creates a Course
    5. Admin assigns the Instructor to the Course
    6. The signed-up Student self-enrolls in the Course
    7. Instructor creates a Section
    8. Instructor adds Markdown content
    9. Instructor creates a Quiz (with duration limit)
    10. Instructor adds multiple-choice questions
    11. Instructor publishes the Quiz
    12. Instructor creates an Announcement
    13. Student retrieves the enrolled Course
    14. Student retrieves the Section and Markdown content
    15. Student retrieves the published Quiz — no correct answers exposed, an attempt is created and the timer starts, expiresAt is returned
    16. Student re-fetches the Quiz before submitting — startedAt/expiresAt is unchanged
    17. Student submits the Quiz attempt — score is auto-calculated and returned
    18. Student attempts to submit the same Quiz a second time — rejected with 409 (single attempt only)
    19. Student retrieves their Quiz result / attempt history
    20. Student retrieves Course grades and the cross-course grade view
    21. Student creates a Discussion post
    22. Instructor (or Admin) replies to the Discussion post
    23. A reply-to-a-reply is attempted and rejected
    24. Student retrieves the Announcement
    25. Student retrieves their Dashboard
    26. Instructor retrieves their Dashboard
    27. Admin retrieves the Admin Dashboard and Course/User data
    ```

    ### Definition of Done

    The API MVP is accepted only when:

    * All required endpoint tests pass.
    * All role-based and resource-ownership authorization tests pass.
    * The complete end-to-end acceptance test above passes.
    * Quiz scores are calculated and persisted correctly, including single-attempt enforcement (DB-level and API-level) and server-side time-limit enforcement based on when the student first viewed the quiz.
    * Student grades are retrievable per course and across all enrolled courses.
    * Courses, sections, Markdown content, quizzes, announcements, and discussions are persisted correctly.
    * The full stack (API + database) runs via a single `docker-compose up`, with seed data present on first boot.
    * Swagger documentation covers all MVP endpoints, including signup.
    * No step in the acceptance workflow requires direct database manipulation.
    * Features explicitly out of scope — PDF material, Material Comments, instructor-created courses, course unenrollment, per-question point weighting, multi-instructor courses, nested (multi-level) discussion replies, **multiple quiz attempts per student**, self-service instructor signup, profile-picture file upload, password reset — are **not** required for acceptance.
