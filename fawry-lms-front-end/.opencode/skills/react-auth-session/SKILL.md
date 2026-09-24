---
name: react-auth-session
description: Implements LMS front-end auth — token storage (access in memory, refresh in sessionStorage), single-flight 401 refresh, AuthProvider actions (signup/login/logout/boot restore), RequireAuth/RequireRole guards, role-based nav. Use when touching login, signup, logout, refresh, session restore, route guards, or role-based redirects.
---

# React Auth & Session (LMS)

Truth: `docs/frontend-build.md` §5 (auth) + §6 (shell/routes); endpoint behavior: `docs/lms-endpoints.md` Auth section. Client guards are UX only — the server is authoritative; always handle 401/403 from the API too.

## Token storage

| Token | Where | Rules |
|---|---|---|
| Access (~15 min) | In-memory module var only | Never `localStorage`, never logs, never URLs |
| Refresh (~7 days) | `sessionStorage` | Cleared on logout |

- Logout: best-effort `POST /api/auth/logout`, then discard local copies → `/login`. Server has already invalidated both tokens (clears stored access token; refresh dies with it) — client just stops using them.
- Boot: if refresh token exists → one silent `POST /api/auth/refresh`; failure → clear → `/login`.

## Single-flight refresh (in the API client)

1. Response `401` → if refresh promise in flight, await it; else start refresh.
2. Success → store new access token → replay original request **once**.
3. Failure → clear tokens → `/login?next=<path>`.
4. Never loop: at most one retry per request; concurrent 401s share one refresh call.

`POST /api/auth/refresh` returns a new **access** token only (spec).

## AuthProvider

State: `user | null` (from `GET /api/users/me`), `status: idle|loading|authenticated|anonymous`.

| Action | Endpoint | Notes |
|---|---|---|
| signup | `POST /api/auth/signup` | Fields exactly `fullName,email,password` — **no role field in the form**; server always creates STUDENT |
| login | `POST /api/auth/login` | `401` → "Invalid email or password" — confirm deactivated-account behavior live first (frontend-build.md Appendix A #13) |
| logout | `POST /api/auth/logout` | clear + redirect |
| refresh | `POST /api/auth/refresh` | via client only |

- Signup `409` → inline error on email.
- After auth success → fetch `/users/me` for role + profile (students: enrolled courses) → redirect `/` → `/dashboard`.

## Guards & routing

- `<RequireAuth>`: anonymous → `/login?next=`.
- `<RequireRole roles={[...]}>`: wrong role → 403 page.
- Wrap every non-public route; role-guard admin/instructor sections per the route×role matrix (`docs/frontend-build.md` §6.1).
- Home redirect: guest `/login`, authenticated `/dashboard`.
- Hiding UI ≠ authorization: still handle server 403 gracefully; never send requests the spec forbids for the known role when avoidable.

## Hard rules

- Never render a role selector on public signup.
- Never persist tokens in `localStorage` or expose them to URLs/logs.
- `/profile` edits only `fullName` + `profilePictureUrl` — no role/email/isActive controls.
- Nav links strictly per role matrix (Student never sees `/admin/*`).

## Checklist before done

1. Signup/login/logout/refresh work against live backend; boot restore works.
2. Concurrent 401s → one refresh; no retry loops.
3. Guards + role nav match §6.1 matrix; deep-link 403/redirect paths verified.
4. No token in storage/logs/URL except refresh in sessionStorage.
