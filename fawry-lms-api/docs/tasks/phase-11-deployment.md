# Phase 11 — Deployment & polish (do this once, at the end)

Status legend: `[x]` done, `[ ]` not yet.

- [x] **11.1 Validation sweep**
  `@Valid` + Bean Validation annotations on every request DTO that doesn't have them yet.
  **Acceptance:** for each mutating endpoint, one test sending a blank required field → 400 with `fieldErrors`; an invalid enum value (e.g. bad `role`) and an invalid FK reference (e.g. assigning a non-existent instructor) are rejected with a clear error, not a 500.
  **Commit:** `feat(validation): add request DTO validation across all mutating endpoints`

- [ ] **11.2 Dockerize the app + wire full-stack docker-compose**
  Multi-stage `Dockerfile` (Maven build → JRE runtime, per spec §1); add the `app` service to `docker-compose.yml` alongside the `postgres` service from 0.2, with `depends_on: postgres (service_healthy)` and env vars for the datasource, `JWT_SECRET`, and `ADMIN_SEED_EMAIL`/`ADMIN_SEED_PASSWORD`.
  **Acceptance:** from a clean checkout, a single `docker-compose up` (no separate `mvn spring-boot:run`) brings up both containers; `GET /actuator/health` against the containerized app → 200; no manual setup steps needed.
  **Commit:** `chore(deploy): dockerize app and wire full-stack docker-compose`

- [ ] **11.3 Startup seed data**
  A `DataSeeder` (`CommandLineRunner`) that runs on every boot but only inserts when the database is empty (e.g. guard on `userRepository.count() == 0`) — **not** gated behind a dev-only profile, since it has to produce a working, logged-in-capable stack the first time anyone runs `docker-compose up`. Inserts, in dependency order: 1 Admin (from `ADMIN_SEED_EMAIL`/`ADMIN_SEED_PASSWORD`, defaulting to documented dev values), 2–3 Instructors, 5–8 Students (all bcrypt-hashed, all with a generated DiceBear avatar); 2–3 Courses each assigned to a different seeded instructor; Enrollments with some students in multiple courses; 2–3 Sections + sample MarkdownContent per course; at least 1 published Quiz per course with 3–5 Questions each and correct options marked; a couple of pre-seeded QuizAttempts + QuizAnswers so grade/attempt-history endpoints aren't empty on first look; a few Announcements and DiscussionPosts per course, including at least one reply (to exercise the one-level-nesting constraint).
  **Acceptance:** fresh `docker-compose up` against an empty volume → all the above data exists; stopping and re-running `docker-compose up` against the same (already-seeded) volume → row counts are unchanged, nothing duplicated.
  **Commit:** `feat(seed): implement full startup seed data with idempotency guard`

- [ ] **11.4 README**
  Run instructions (`docker-compose up` as the only required step), env vars, a "Seeded accounts" table (all roles, from 11.3), swagger URL.
  **Acceptance:** a person with a clean checkout can follow it start-to-finish with no missing steps, and log in as any seeded role via Swagger using only the README.
  **Commit:** `docs: add README with setup, run instructions, and seeded accounts`
