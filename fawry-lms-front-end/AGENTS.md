# AGENTS.md — University LMS Front-End

Instructions for any agent/teammate working in this repo. Persist everything load-bearing here.

## Source of truth (in priority order)

1. **`docs/frontend-build.md`** — the build plan: scope, stack, auth model, API layer rules, route×role matrix, feature guides §7.x, status→behavior table §8.1, build order §9, verification walkthrough §10, Appendix A (shapes to confirm live).
2. **`docs/lms-endpoints.md`** — every endpoint, its access rule, and the quiz-attempt state machine. Its Access column **is** the authorization spec.
3. **`docs/overview.md`** — product description (nine functional areas, three roles).

Response-body field names not pinned by these docs come **only** from the generated `src/api/schema.d.ts` (OpenAPI codegen from the running backend). **Never guess a field name.**

Do not treat any file not listed above (or a live API response) as authoritative. Skills under `.opencode/skills/` summarize the same rules — the docs win on conflict.

## Hard rules (violating these breaks the product)

- **Quiz take (student):** `GET /api/quizzes/{id}` has a side effect — it creates the single attempt and starts the timer. Fire it only after an explicit **Start quiz** click, or when resuming an already-created unsubmitted attempt (detect first via `GET /api/quizzes/{id}/attempts/me`, which is side-effect-free). No prefetch, no hover-fetch, no route loaders, no caching for that query (`staleTime:0`, `gcTime:0`). Staff and student quiz views are **separate components**. Never render correctness info to a student before submit. Never render a retake button. Server clock is the only authority for the deadline.
- **Tokens:** access token in-memory only (module variable); refresh token in `sessionStorage`. Never `localStorage`, never logs, never URLs.
- **Signup:** fields are exactly `fullName`, `email`, `password`. No role selector anywhere — server hardcodes STUDENT.
- **Paths:** every endpoint path is copy-pasted verbatim from `lms-endpoints.md` including its leading `/api/`. `VITE_API_BASE_URL` is host only; do not strip or re-add `/api` in the client.
- **Types:** only the spec-pinned shapes may be coded before `schema.d.ts` exists (auth bodies, `{instructorId}`, quiz `expiresAt`|`secondsRemaining`). Everything else reads `schema.d.ts`.
- **Timestamps:** treat as UTC ISO-8601; display via `Intl.DateTimeFormat` local zone. Never send client timestamps for quiz deadlines.
- **Markdown:** render with `react-markdown` + `remark-gfm` — never `dangerouslySetInnerHTML`.
- **Authorization UI is UX only:** hiding a button is not security; the server is authoritative. Every screen handles 401/403 per `frontend-build.md` §8.1 regardless of guards.
- **Ownership:** "Own(Instructor)" resolves via the **course's** assigned instructor id, never via child resources.
- **Out of scope — do not build:** password reset, file upload, unenroll, multi-level replies, non-markdown content, email/notifications, invented dashboard widgets or grade aggregates.

## Stack

Vite 8 + React 19 + TypeScript 7, **`react-router` v8** (declarative mode — `react-router-dom` does not exist in v8), `@tanstack/react-query` v5, Tailwind CSS v4 (`@tailwindcss/vite`, CSS-first, no config file), `openapi-typescript` v7, `react-markdown` + `remark-gfm`. Install with `@latest` at scaffold time; the lockfile is the record of what landed.

## Commands

```bash
npm run dev        # dev server
npm run build      # tsc -b && vite build — must stay green
npm run lint       # oxlint (current Vite react-ts template default — not ESLint)
npm run format     # prettier
npm run gen:api    # openapi-typescript http://localhost:8080/v3/api-docs -o src/api/schema.d.ts (backend must be up)
```

Note: `gen:api` embeds the dev backend host because npm scripts run under cmd on Windows, which cannot expand `$VITE_API_BASE_URL`. This is a dev-time codegen script only — runtime code reads `import.meta.env.VITE_API_BASE_URL` exclusively.

Backend: `http://localhost:8080` (see `.env.development`). Re-run `gen:api` whenever the contract changes and commit `schema.d.ts`.

## Layout

`src/api` (client, ApiError, Page, keys, schema) · `src/auth` (AuthProvider, token store, refresh, guards) · `src/routes` · `src/components` (shared UI) · `src/features/<name>` (vertical slices: `api.ts`, `queries.ts`, components, screen). No `fetch` outside `src/api` / `features/*/api.ts`.

## Work loop

For each build-order step in `frontend-build.md` §9:

1. **Plan** — restate the step's section + acceptance boxes.
2. **Implement** — smallest coherent change; follow the matching skill in `.opencode/skills/` (`react-api-layer`, `react-auth-session`, `react-feature-slice`, `react-quiz-attempt`).
3. **Verify** — `npm run lint` && `npm run build`; live API checks where the section requires them; exercise at least one error path (403/409) per feature.
4. **Commit** — one conventional commit per step (`feat(scope): …`, `chore: …`, `docs: …`); tick the section's acceptance boxes in `docs/frontend-build.md` in the same commit.

## Appendix A — confirm live before hardening a UI rule

Page field names · token key names · submit body + score payload · `expiresAt` vs `secondsRemaining` · staff quiz GET behavior · multi-correct scoring · dashboard DTOs · `users/me` enrolled-courses field · section `orderIndex` on create · discussion reply nesting · admin create-user DTO · deactivated-login status · section-delete cascade · admin exemption from enrollment checks · assign-instructor response · `ApiErrorBody`/`fieldErrors` shape.
