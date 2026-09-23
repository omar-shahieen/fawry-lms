---
name: spring-boot-persistence
description: Designs JPA entities per LMS indexing, uniqueness, soft-delete, and UTC rules under ddl-auto update. Use when creating or modifying entities, repositories, indexes, unique constraints, or timestamp fields.
---

# Spring Boot Persistence (LMS)

PostgreSQL is the source of truth. Hibernate `ddl-auto: update` (deliberate — no Flyway). Preserve every uniqueness constraint used for concurrency.

## Entity checklist

- **FK indexes:** every `@ManyToOne` FK column needs an explicit `@Index` — Postgres does **not** auto-index FKs (spec §2a “must-index” list).
- **Unique login/domain keys:** `User.email`, `Course.code` → `unique = true`.
- **Concurrency unique constraints** (correctness, not just perf):
  - `Enrollment(student_id, course_id)`
  - `QuizAttempt(quiz_id, student_id)`
- **Composite indexes** where query shape is known (spec §2a): e.g. `Section(course_id, orderIndex)`, `Quiz(course_id, published)`, `DiscussionPost(course_id, createdAt)`, `Announcement(course_id, createdAt)`.
- **Soft delete:** `isActive` boolean default `true` on User/Course; filter active rows on nearly every list query.
- **Timestamps:** store/compare in **UTC**; `createdAt` / `updatedAt` per existing entity style.
- Declare indexes via `@Table(indexes = …)` / `@UniqueConstraint` so `ddl-auto: update` can create them.

## Hard rules

- **Never remove or relax a uniqueness constraint** to make an operation succeed — it is the race-condition guard (double enroll, double quiz attempt).
- App-layer checks are not enough for those races: rely on the DB constraint; map `DataIntegrityViolationException` → **409** in `GlobalExceptionHandler`.
- Do not add Flyway/Liquibase, partial-index raw SQL, or cascade behaviors the spec does not ask for.
- Deactivating a parent does not cascade-deactivate children (deliberate simplicity).
- Do not invent entities/fields not in the spec’s entity model (§2).

## Repositories

- Spring Data methods / `existsBy…` for enrollment and ownership probes.
- Keep query logic in repositories/services — no business rules in entities beyond what the existing model already does.

## When schema conflicts appear

1. Confirm whether the constraint/index is load-bearing per spec §7.4.
2. Fix application logic to satisfy the constraint.
3. Only change schema if the spec requires it — and call that out in the report.

## Tests

Prove uniqueness races with the patterns in `spring-boot-testing` (constraint-driven 409, concurrent submit → one row).
