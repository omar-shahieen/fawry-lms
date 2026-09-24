---
name: react-quiz-attempt
description: Implements the student quiz-taking screen for the LMS — GET /api/quizzes/{id} side effect (attempt starts on first student GET), no-prefetch/no-cache rules, countdown from expiresAt, submit with 400/409/late handling, post-submit review mode. Use when building or touching the quiz take screen, quiz detail fetch, submit flow, timer, or attempt review.
---

# React Quiz Attempt (LMS) — critical

Truth: `docs/frontend-build.md` §7.6; backend behavior: `docs/lms-endpoints.md` Quiz Attempt Flow. Violating the client rules breaks the product (starts the timer too early or leaks answers).

## The side effect

For a **STUDENT**, `GET /api/quizzes/{id}` (enrollment-checked):

- No attempt → **creates** `QuizAttempt`, `startedAt=now()`, returns `expiresAt`/`secondsRemaining`.
- Unsubmitted attempt → returns as-is; `startedAt` never resets (re-GET is deadline-safe but must not be fired casually — see rules).
- Submitted → read-only review: own answers + score. No second attempt ever.
- Pre-submit payload **never** includes `isCorrect`.
- Instructor/Admin GET does not create an attempt; staff view may include `isCorrect` — keep staff preview and student take in **separate components**.

`GET /api/quizzes/{id}/attempts/me` has **no** side effect — use it to detect state before deciding to fetch the quiz.

## Hard client rules

1. Student `GET /quizzes/{id}` fires **only** after explicit **Start quiz** click, or when resuming an already-created unsubmitted attempt (detected via `/attempts/me` first).
2. **No prefetch, no hover-fetch, no route loader, no cache** for this query as student: `staleTime:0`, `gcTime:0`, no persistence, refetch on mount. Never add it to query-key lists that get prefetched or invalidated en masse.
3. Clock is **server time**: countdown = `expiresAt - Date.now()`, tick 1s. Never trust/extend local elapsed time; server rejects late submits on its own clock anyway.
4. Single submit: disable button while in flight; on `409` go to review; never auto-retry submit.
5. After submit, GET is safe (review) — still avoid refetch loops; invalidate `grades`, `dashboard`, attempts keys on success.

## Screen flow

1. Open `/quizzes/:id` → `attempts/me`:
   - **None + never started** → landing card: title, question count, `durationMinutes`, warning "Starting creates your single attempt and starts the timer" → **Start quiz** → `GET /quizzes/{id}` → store `expiresAt` → render.
   - **Unsubmitted attempt exists** → `GET /quizzes/{id}` → same countdown as first fetch (server did not reset).
   - **Submitted** → `GET /quizzes/{id}` → read-only review (own answers, score, correctness + correct answers); no submit button.
2. Taking: checkboxes if the question has multiple correct options (radio only when exactly one is correct — confirm via `schema.d.ts`); sticky countdown; at expiry disable inputs + "Time is up — submit now."
3. Submit `POST /quizzes/{id}/submit` (body shape from `schema.d.ts`): success → `score/totalQuestions` + per-question review → invalidate grades/dashboard.
4. Errors: `400` no attempt → recover via flow 1; `409` → "already submitted — single attempt only" → review; late-reject → "time limit passed — not accepted" → offer review.

## Never

- Never call `GET /quizzes/{id}` from the course page, quiz list card, hover, or prefetch for a student.
- Never render `isCorrect` (or any correct-flag) before submission.
- Never show a second-attempt or retake button.
- Never invent a client-side deadline extension.

## Checklist before done (§7.6.2a–f)

1. Attempt created only via Start click (network tab proof).
2. Reload mid-attempt keeps same `expiresAt`.
3. No `isCorrect` in pre-submit DOM/response used for render.
4. Submit → score+review; second submit → 409 copy, stays in review.
5. Countdown zero disables inputs.
6. Post-submit visits = permanent review; `/attempts/me` shows stored attempt.
