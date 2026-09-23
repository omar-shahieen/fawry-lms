---
name: spring-boot-testing
description: Writes and runs LMS tests in priority order — authorization matrix, quiz-attempt state machine, DB uniqueness races, then happy paths — and maps acceptance criteria to assertions. Use when writing tests, verifying acceptance criteria, or before declaring a task done.
---

# Spring Boot Testing (LMS)

Test what breaks correctness first. Full coverage is not the goal for this assessment; the high-value invariants are.

## Priority order

1. **Authorization matrix** — per restricted-endpoint pattern, allow **and** deny for each relevant role (ADMIN, INSTRUCTOR, STUDENT; owner vs non-owner; enrolled vs not).
2. **Quiz-attempt state machine** (spec §3):
   - First student `GET` creates exactly one `QuizAttempt`, returns `expiresAt`/`secondsRemaining`.
   - Repeat `GET` does **not** reset `startedAt`.
   - Student payload pre-submit never includes `isCorrect`.
   - Post-submit `GET` returns stored answers/score, still one row.
   - Submit with no attempt → 400; already submitted → 409; past deadline (server clock / manipulated `startedAt` fixture) → rejected.
   - Two near-simultaneous submits → exactly one graded row; loser 409 (DB constraint is the guard).
3. **DB uniqueness/concurrency** — enrollment double-submit → 409; constraints never weakened.
4. **Core happy paths** when time permits.

## Mapping acceptance criteria → tests

Phase steps in `docs/tasks/` end with **Acceptance:** lines that already name the proof. For each bullet write an assertion that fails if the behavior regresses (status code, row count, field presence/absence). Do not mark a step `[x]` until those assertions pass.

## Style

- Prefer integration tests (MockMvc / `@SpringBootTest` + database) for endpoint behavior — that is what acceptance criteria describe.
- Seed fixtures explicitly (users, course, enrollment, quiz) so auth and enrollment branches are deterministic.
- For deadline tests, manipulate `startedAt` in the fixture — never trust a client timestamp.
- For race tests, fire concurrent requests and assert **one** persisted row + 409 for the loser.
- Auth tests must not disable security; exercise real `@PreAuthorize` paths with properly issued auth (test tokens or `WithMockUser` consistent with how authorities are named `ROLE_*`).

## Running

```bash
./mvnw test
./mvnw verify   # before declaring the implementation complete
```

- Run the tests covering your change after every behavioral change.
- If a command cannot be run (missing Docker, etc.), say so explicitly in the report — do not claim green.

## Done means

- Acceptance criteria for the step converted to passing tests (or an explicit, justified gap).
- No authz regressions (deny paths still deny).
- Report lists tests run, results, and anything skipped.
