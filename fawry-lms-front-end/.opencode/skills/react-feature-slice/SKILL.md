---
name: react-feature-slice
description: Adds a feature screen to the LMS React front-end as a vertical slice (api.ts, queries/mutations, UI states, route wiring, acceptance checkboxes) with Tailwind + URL-synced pagination and the §8.1 status→behavior table. Use when building any feature screen (courses, sections, grades, discussion, dashboards) or adding routes/components.
---

# React Feature Slice (LMS)

Source of truth: `docs/frontend-build.md` §7 (per-feature guides) + §8 (cross-cutting conventions). Copy the nearest completed feature's structure — do not introduce new state libs or folder styles. The backend is complete — wire real API calls directly.

## Slice layout

```
src/features/<name>/
├── api.ts          # calls via shared client only
├── queries.ts      # query/mutation hooks + keys from src/api/keys.ts
├── components/     # screen pieces
└── <Screen>.tsx    # composes the above
```

Route added in `src/routes/` per the route×role matrix (`docs/frontend-build.md` §6.1) with `RequireAuth` / `RequireRole`.

## Slice rules

| Layer | Rule |
|---|---|
| api.ts | Shared client only; types from `schema.d.ts`; no fetch in components |
| queries.ts | TanStack Query hooks; centralized keys; mutations invalidate exactly what changed |
| Lists | Shared `Pagination`; **URL search params** hold page/size/sort/filters |
| Guard UI | Hide actions the known role can never perform; still handle server 403 |
| Styling | Tailwind utilities; shared components from `src/components/` (Button, Input, Modal, EmptyState, Pagination) |

## Every data region defines 4 states

1. **Loading** — skeleton (not a spinner-only page).
2. **Empty** — `EmptyState` + CTA where sensible.
3. **Error** — message + retry; `ApiError` status per §8.1 table (400 fieldErrors inline · 401 refresh/login · 403 forbidden state/page · 404 empty/page · 409 feature copy · 500 toast+retry).
4. **Forbidden** — common student case: not enrolled → "You are not enrolled in this course" + link to course (enroll CTA if applicable).

Status→behavior: `docs/frontend-build.md` §8.1 (400 fieldErrors inline · 401 refresh/login · 403 forbidden state/page · 404 empty/page · 409 feature copy · 500 toast+retry).

## Contract discipline

- Endpoint, method, access from `docs/lms-endpoints.md`; feature behavior from the matching §7.x guide. Cite the section in the completion report.
- Do not invent fields beyond `schema.d.ts` (dashboards: only regions listed in §7.10 — no progress %, no announcement feed, no GPA).
- Timestamps are UTC → display via `Intl.DateTimeFormat`; never send client timestamps for deadlines.
- Markdown bodies render with `react-markdown` (+ GFM) — **never** `dangerouslySetInnerHTML`.
- One-level discussion: Reply control only on top-level posts; edit/delete visibility per author/admin rules (§7.8).
- Enrollment is enroll-only — no unenroll UI.

## Special slices

- **Quiz take** → follow `react-quiz-attempt` instead of generic query rules (no cache/prefetch).
- **Auth screens** → follow `react-auth-session` (no role field on signup).

## Checklist before done

1. Types from codegen (`schema.d.ts`), never hand-guessed.
2. Route + guards match §6.1 matrix; nav visibility correct.
3. Four UI states implemented; §8.1 error behaviors exercised (force a 403/409 once).
4. Lists URL-synced; mutations invalidate correct keys.
5. Feature's §7.x acceptance boxes checked.
6. No out-of-scope UI (§1) slipped in; spec section cited in report.
