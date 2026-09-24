---
name: react-api-layer
description: Builds the typed API layer for the LMS React front-end — openapi-typescript codegen from /v3/api-docs, fetch client with single-retry refresh hook, ApiError with fieldErrors, Spring Page helper, TanStack Query keys/defaults. Use when adding API calls, generating types, handling errors/pagination, or setting up query keys.
---

# React API Layer (LMS)

Source of truth for the contract: `docs/frontend-build.md` §4; endpoint/access truth: `docs/lms-endpoints.md`. Never invent field names — they come from codegen or the few shapes the spec pins.

## Type generation

```bash
npm run gen:api   # openapi-typescript "$VITE_API_BASE_URL/v3/api-docs" -o src/api/schema.d.ts
```

- Re-run whenever the backend adds endpoints; commit `schema.d.ts`.
- Spec-pinned shapes only (safe before backend exists): error body `{timestamp,status,error,message,fieldErrors?}`, auth bodies `{fullName,email,password}` / `{email,password}`, `{instructorId}`, quiz `expiresAt`/`secondsRemaining`.
- Everything else (Page field names, submit body, dashboard DTOs, token key names) → read `schema.d.ts`, see `docs/frontend-build.md` Appendix A. Do not guess.

## Client (`src/api/client.ts`)

1. Prefix `import.meta.env.VITE_API_BASE_URL` — no other hardcoded hosts.
2. `Authorization: Bearer <accessToken>` when in memory.
3. Success → parse JSON (handle `204`/empty); failure → throw `ApiError` (never return raw Response).
4. On `401` → call the shared single-flight refresh (from the `react-auth-session` skill), replay original **once**; refresh fail → clear session, throw.

No `fetch` inside JSX or components — feature calls live in `src/api/` or `src/features/<f>/api.ts`.

## Errors

```ts
class ApiError extends Error {
  status: number;
  fieldErrors?: Record<string, string>;
}
```

- `fieldErrors` → inline form errors (never toast-only).
- Status mapping lives in one place (`docs/frontend-build.md` §8.1 table): 400/422 inline · 401 refresh→login · 403 page/state · 404 page/empty · 409 feature-specific copy · 500 toast+retry.

## Pagination & lists

- `Page<T>` helper for Spring list endpoints; shared `Pagination` component.
- List state in **URL search params** (`page/size/sort` + `search/term/code/role`), not component state.
- Filters combinable only where the endpoint allows (courses: search+term+code; users: role).

## TanStack Query

- Defaults: `staleTime` ~30s, no retry on 4xx (≤2 on network/5xx), `refetchOnWindowFocus: false`.
- Query keys centralized in `src/api/keys.ts` (`['courses', filters]`, `['quiz', id]`, …); mutations invalidate exact keys.
- **Exception:** student quiz-detail query — zero cache, no prefetch (`gcTime:0`, `staleTime:0`) — see `react-quiz-attempt`. Never enable route prefetch or hover-fetch for it.

## Checklist before done

1. Types from `schema.d.ts`, not hand-written guesses.
2. All calls through the shared client; errors are `ApiError`.
3. Paginated lists use URL-synced shared Pagination.
4. Keys centralized; mutations invalidate.
5. Quiz-detail query caching rules untouched.
