# Phase 12 — End-to-end acceptance test

Status legend: `[x]` done, `[ ]` not yet.

- [x] **12.1 Automated end-to-end acceptance test**
  A single ordered integration test (MockMvc/RestAssured against the full running app, HTTP calls only — no direct DB manipulation) implementing the 27-step sequence from the acceptance doc: seed data present → student signup (role field ignored) → admin creates instructor → admin creates course → admin assigns instructor → student self-enrolls → instructor creates section → adds markdown content → creates quiz → adds questions → publishes → creates announcement → student reads course/section/content → student's first quiz GET creates an attempt and starts the timer, `expiresAt` returned → student re-fetches before submitting, `startedAt`/`expiresAt` unchanged → student submits, score auto-calculated → second submit rejected with 409 → student reads attempt history → student reads course grades and the cross-course grade view → student creates a discussion post → instructor/admin replies → reply-to-a-reply rejected → student reads the announcement → student dashboard → instructor dashboard → admin dashboard + course/user data.
  This is the closing item in the acceptance doc's Definition of Done, so it runs last, once every endpoint above exists.
  **Acceptance:** the test passes against a freshly built app in one run, covering all 27 steps with an assertion each.
  **Commit:** `test(e2e): add full acceptance-flow end-to-end test`
