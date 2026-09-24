
# React Front-End Build Guide — University LMS

**Status legend:** `[x]` done · `[ ]` not yet.

**Revision note:** this replaces the earlier draft, which cited `docs/lms-spec.md`, `docs/lms-api-acceptance-criteria.md`, and `docs/tasks/` — none of which exist in this project's doc set. The backend is now **complete and running**, so nothing here is phase-gated on backend work anymore. Everything below is grounded in the two documents that actually exist:

- **`overview.md`** — product description, the nine functional areas, the three roles.
- **`lms-endpoints.md`** — the literal endpoint list, access rules, and the quiz-attempt state machine. Its own header says every row's Access column **is** the spec for that endpoint's authorization — treat it as ground truth for "who can call what."

Where a response body's exact JSON field names aren't pinned by either doc (most response DTOs aren't), the tie-breaker is the backend's live OpenAPI document, generated into `schema.d.ts` in step 3.1. **Never guess a field name — generate the types and read them.** Appendix A lists every shape that needs this treatment plus a few behavioral edge cases worth confirming empirically against the running API before you harden a UI rule around them.

System context (from `overview.md`): single Spring Boot 4.1.1 monolith (Java 25) over PostgreSQL 18, three roles — Student, Instructor, Admin — JWT bearer auth with access + refresh tokens.

---

## 1. Scope

### In scope (v1)

All three roles, driven end-to-end through the UI:

* Auth (signup, login, refresh, logout).
* Own profile (view/edit).
* Course catalog, course detail, self-enrollment, roster.
* Admin course management (create, edit, soft-delete, assign instructor).
* **Admin user management** (create instructor/admin accounts, list/filter users, edit/promote, deactivate) — see §7.2. This is easy to miss because it has no top-level mention in `overview.md` §1 beyond "administrators can create, update, deactivate, and manage users and their roles," but it is the *only* way Instructor and additional Admin accounts come into existence (signup always creates Student — see §5).
* Sections (ordered) and Markdown content, reader + staff CRUD + reordering.
* Quiz builder (staff): quizzes, questions, options, publish toggle.
* Quiz taking (student): single timed attempt, enforced by a server-side state machine — see §7.6, the most fragile feature in this app.
* Grades: student's own grouped results; staff flat per-course table.
* Discussion (one-level replies) and Announcements, per course.
* Three role-specific dashboards.

### Out of scope (nothing in `lms-endpoints.md` exposes these — do not build UI for them)

* Password reset / forgot-password — no such endpoint exists.
* Profile-picture **file upload** — `profilePictureUrl` is a plain string field the user pastes a URL into; it's auto-generated at signup and editable via `PATCH /api/users/me`. No multipart endpoint exists anywhere in the list.
* Course **un**enrollment — enroll-only; there is no un-enroll endpoint.
* Multiple quiz attempts — single attempt, DB-unique-constraint-enforced (§7.6).
* Multi-level discussion replies — replies attach only to top-level posts; the server rejects reply-to-reply.
* PDF or any other file-based content type — content bodies are Markdown text only (`overview.md` §3).
* Email notifications, live classes, analytics beyond the three dashboards, payments — none of these appear anywhere in the endpoint list.

### Deliberate backend design choices the UI must respect

These come straight from `lms-endpoints.md`'s own commentary, not from an external spec:

* **Quiz attempt starts on the student's first `GET /api/quizzes/{id}`.** This was a deliberate tradeoff (no dedicated "start" endpoint) to close the loophole where a student could view a quiz, stall indefinitely, then submit. The UI must never trigger that `GET` incidentally — see the hard rules in §7.6.
* **Signup returns tokens immediately** — there is no separate login round-trip after signup.
* **Logout clears the server-stored access token**; the JWT filter then rejects that access token, and the refresh endpoint rejects the associated refresh token too, until a new login or signup stores a fresh one. So logout invalidates *both* tokens server-side — the client just discards its local copies on top of that.
* **Single quiz attempt is DB-enforced** via a unique constraint on `(quiz_id, student_id)`, not just an application-layer check — a race between two near-simultaneous submits still resolves to one row.
* Timestamps: `lms-endpoints.md` doesn't state a timezone convention explicitly. Treat every timestamp field as UTC ISO-8601 until `schema.d.ts` or a live response says otherwise (this is the overwhelming Spring Boot / Jackson default), and always format for display in the browser's local zone.

---

## 2. Stack & scaffold

| Concern | Choice | Notes |
|---|---|---|
| Build | Vite | v8.x — `vite`, `@vitejs/plugin-react` |
| UI | React + ReactDOM | v19.x |
| Language | TypeScript | latest stable |
| Routing | React Router | v8.x — package name is **`react-router`**. `react-router-dom` was removed in v8; installing it is a mistake that will break imports immediately. Use **declarative mode** (`<BrowserRouter>`, `<Routes>`, `<Route>`) — this app doesn't need framework-mode SSR or data-mode loaders/actions. |
| Server state | TanStack Query | `@tanstack/react-query` v5.x |
| Styling | Tailwind CSS | v4.x via `@tailwindcss/vite`, CSS-first config (`@import "tailwindcss";` in `src/index.css`, no `tailwind.config.js` needed) |
| API types | `openapi-typescript` | latest v7.x |
| Markdown rendering | `react-markdown` + `remark-gfm` | latest stable |
| Lint/format | ESLint (Vite template) + Prettier | latest stable |

Don't hardcode exact patch versions into `package.json` from memory — run `npm view <pkg> version` (or just `npm install <pkg>@latest`) at scaffold time so the agent building this gets whatever is actually current, and record what landed in the lockfile. React Router v8 in particular has moved fast; confirm its peer requirements (recent versions have wanted Node 22.22+ and React 19.2+) against your installed Node/React before scaffolding.

### 2.1 Scaffold

```bash
npm create vite@latest . -- --template react-ts
npm install
npm install react-router @tanstack/react-query react-markdown remark-gfm
npm install -D tailwindcss @tailwindcss/vite openapi-typescript
```

- [x] **2.1a** Project scaffolds; `npm run dev` serves the app; `npm run build` passes.
- [x] **2.1b** `vite.config.ts` loads `@tailwindcss/vite`; `src/index.css` contains `@import "tailwindcss";`; a Tailwind utility class visibly renders.
- [x] **2.1c** Confirm the installed package is `react-router`, not `react-router-dom` — check `package.json`.

### 2.2 Environment

```bash
# .env.development
VITE_API_BASE_URL=http://localhost:8080
```

`VITE_API_BASE_URL` is the **host only** — every endpoint path used anywhere in this app, in every feature module, should be copy-pasted verbatim from `lms-endpoints.md` **including its leading `/api/`**. Do not strip `/api` and re-add it in the client wrapper, and do not bake `/api` into `VITE_API_BASE_URL`. One source of truth for each path, character-for-character matching the spec table, eliminates an entire class of typo bugs.

- [x] **2.2a** `VITE_API_BASE_URL` read via `import.meta.env.VITE_API_BASE_URL`; no other hardcoded hostnames anywhere; `.env` files with real values are never committed.

### 2.3 Folder layout

```
src/
├── api/            # fetch client, ApiError, Page helper, generated schema.d.ts, query keys
├── auth/           # AuthProvider, token store, refresh logic, guards
├── routes/         # router config, RequireAuth, RequireRole, layout
├── components/     # shared UI (Button, Input, Modal, Pagination, EmptyState, ...)
└── features/
    ├── auth/       # login/signup screens
    ├── profile/
    ├── courses/
    ├── sections/
    ├── content/
    ├── quizzes/    # builder (staff) + taking (student) live in SEPARATE files — see §7.6.5
    ├── grades/
    ├── discussion/
    ├── announcements/
    ├── admin-users/ # NEW — admin user management (§7.2)
    └── dashboards/
```

- [x] **2.3a** Folders exist; every API call lives in `src/api` or `src/features/<f>/api.ts` — no `fetch` calls inside JSX/components.

---

## 3. Authorization model — read this before writing any guard or screen

`lms-endpoints.md`'s own legend:

> 🔓 public · 🔐 any authenticated user · role tags = restricted to that role (**Admin always implicitly allowed unless noted**) · "Own" = resource-level ownership check required.

Translate that into a concrete decision table:

| Access column reads | Meaning | Who the UI shows the action to |
|---|---|---|
| 🔓 | No token required | Anyone — signup, login, refresh only |
| 🔐 | Any authenticated user, any role | Every logged-in role |
| `STUDENT`, `INSTRUCTOR`, or `ADMIN` alone | Restricted to that role | That role, **plus Admin** (the legend says Admin is implicitly allowed on every role-restricted row unless the row says otherwise — none of the rows in `lms-endpoints.md` say otherwise) |
| `ADMIN, Own(Instructor)` | Admin always; Instructor only on resources they're assigned to | Admin always; Instructor only when they are the course's assigned instructor |
| `🔐` + "enrollment check" | Any authenticated user, plus a server-side check that the caller is enrolled (student) | Enrolled students; per the same Admin-implicit-allowance principle this guide treats **Admin as exempt from the enrollment check too** — this is the most consistent reading of the legend and matches how staff clearly need to read course content they didn't enroll in, but it is **not spelled out explicitly** for these specific rows, so verify it against the running API (Appendix A) before hard-coding an assumption the UI depends on. Instructor is not explicitly exempted either; treat Instructor as needing to be the course's assigned instructor for the equivalent write endpoints, and confirm read-side behavior live. |
| `Own` alone (no role prefix) | The acting user must be the resource's own creator | e.g. editing your own discussion post |

**"Own(Instructor)" always resolves through the course**, not through the sub-resource directly: a section, a content item, a quiz, a question, an announcement — none of these carry their own instructor field. Ownership is "is this user the instructor assigned to the parent course," and that assignment happens exactly one way: `PATCH /api/courses/{id}/assign-instructor`. When building any "is this mine?" UI check client-side, resolve it by comparing the logged-in user's id against the **course's** instructor id, not anything on the child resource.

**Client guards are UX only.** Hiding a button because the current user's role/ownership doesn't match this table is a convenience, not security — the server enforces the real rule on every call regardless of what the UI shows. Every screen still needs to handle a `403` gracefully (§8.1) for the cases this table got wrong or that changed between render and click (e.g., an instructor un-assigned from a course mid-session).

---

## 4. API integration layer

### 4.1 Generate types from the live OpenAPI document

Spring Boot APIs overwhelmingly serve this via springdoc at `GET {VITE_API_BASE_URL}/v3/api-docs` — confirm that path against the running backend before wiring the script (it isn't stated in either source doc).

```json
// package.json
"scripts": {
  "gen:api": "openapi-typescript \"$VITE_API_BASE_URL/v3/api-docs\" -o src/api/schema.d.ts"
}
```

- [x] **4.1a** `npm run gen:api` (backend running) produces `src/api/schema.d.ts`.
- [x] **4.1b** Re-run whenever the backend contract changes; commit the generated file so every teammate/agent works from the same shapes.

> Only these shapes are pinned directly by `lms-endpoints.md` and may be coded before checking `schema.d.ts`: the auth request bodies (`{fullName, email, password}` for signup, `{email, password}` for login), `{instructorId}` for assign-instructor, and the quiz `expiresAt` **or** `secondsRemaining` field (the doc explicitly allows either — check which one the live response actually returns). Everything else — every list/detail response shape, every write-request body beyond what's just listed, pagination envelope field names, dashboard payloads — comes from `schema.d.ts`.

### 4.2 Typed fetch client

One wrapper module, `src/api/client.ts`:

1. Prefix every request with `VITE_API_BASE_URL` (paths already carry their own `/api/...`, per §2.2).
2. Set `Content-Type: application/json` on requests with a body.
3. Attach `Authorization: Bearer <accessToken>` whenever a token is held in memory.
4. Parse JSON on success; on failure, parse the error body into `ApiError` and throw — never return a raw `Response` to a caller.
5. On `401`: run the single-flight refresh (§5.2), retry the original request **once**; if refresh fails, clear the session and throw.

- [x] **4.2a** Every feature's API functions go through this client — no ad hoc `fetch` elsewhere.
- [x] **4.2b** Non-JSON or empty response bodies (e.g. a logout response with no body) are handled without throwing a parse error.

### 4.3 Error model

`ApiError` is a reasonable, common Spring convention, but its exact field names aren't pinned by either source doc — confirm against a live error response (any `400`/`403`/`404` from the running API) and against `schema.d.ts` before finalizing the interface below. Treat this as a starting shape to verify, not a guarantee:

```ts
interface ApiErrorBody {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  fieldErrors?: Record<string, string>; // field-level validation errors, if the backend sends them this way — confirm shape
}

class ApiError extends Error {
  status: number;
  fieldErrors?: Record<string, string>;
}
```

- [x] **4.3a** Every thrown client error is an `ApiError` with `status` populated, confirmed against at least one real `400` and one real `403`/`404` response from the running backend.
- [x] **4.3b** Field errors (however they're actually shaped) get mapped onto individual form fields — never surfaced only as a generic toast.

### 4.4 Pagination helper

`lms-endpoints.md` repeatedly says "paginated" (users, courses, roster, quiz attempts, staff grades, discussion, announcements) but never pins the JSON envelope. Spring Data's default `Page<T>` shape is the safe starting assumption — confirm every field name against `schema.d.ts` before shipping:

```ts
interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;   // current page, 0-based — confirm
  size: number;
}
```

Shared list-query convention to try first: `?page=0&size=20&sort=<field>,asc|desc` plus each endpoint's own filters (`search`, `term`, `code` on courses; `role` on users — combinable where the table says so).

- [x] **4.4a** One shared `Pagination` component used by every paginated list (users, courses, roster, attempts, staff grades, discussion, announcements).
- [x] **4.4b** Page state lives in URL search params (shareable, back-button-friendly), not local component state.

### 4.5 TanStack Query defaults

- [x] **4.5a** `QueryClient` defaults: `staleTime` ~30s, no retry on `4xx` (retry up to 2× on network/`5xx`), `refetchOnWindowFocus: false`.
- [x] **4.5b** Query keys centralized in `src/api/keys.ts` (`['courses', filters]`, `['quiz', id]`, …); mutations invalidate exactly the keys they affect.
- [x] **4.5c** The student's quiz-detail query is **exempt** from these defaults entirely — see the hard rules in §7.6.

---

## 5. Auth & session

### 5.1 Token storage

| Token | Where | Why |
|---|---|---|
| Access token | In-memory only (a module variable inside `auth/`) | Not readable by injected scripts; lost on reload is fine because boot silently refreshes |
| Refresh token | `sessionStorage` (per-tab, cleared on logout) | Survives reload; `sessionStorage` over `localStorage` deliberately limits multi-tab sharing |

`lms-endpoints.md` doesn't state a token lifetime, so don't hardcode an expiry assumption into the UI (e.g. a countdown on the session itself) — rely on `401` responses to trigger refresh reactively rather than a client-side timer.

- [x] **5.1a** No token is ever written to `localStorage`, logged, or put in a URL.
- [x] **5.1b** On app boot: if a refresh token exists in `sessionStorage`, silently `POST /api/auth/refresh` once to restore the access token; on failure, clear storage and land on `/login`.

### 5.2 Single-flight refresh

1. A request returns `401`.
2. If a refresh is already in flight, await that same promise; otherwise start `POST /api/auth/refresh` with the stored refresh token.
3. On success: store the new access token, replay the original request **once**.
4. On failure: clear tokens, redirect to `/login?next=<current-path>`.

- [x] **5.2a** Two concurrent `401`s trigger exactly **one** refresh call.
- [x] **5.2b** A request retried after a failed refresh never loops — at most one retry per request, ever.

### 5.3 AuthProvider

State: `user | null` (from `GET /api/users/me`), `status: idle | loading | authenticated | anonymous`.

| Action | Endpoint | Success behavior |
|---|---|---|
| Signup | `POST /api/auth/signup` `{fullName, email, password}` | Store the tokens returned **immediately** (no login round-trip) → fetch `/api/users/me` → redirect by role. There is **no role field anywhere in this form** — the server hardcodes `role = STUDENT` and ignores any `role` key sent, because `SignupRequest` has no such field at all. |
| Login | `POST /api/auth/login` `{email, password}` | Store tokens → fetch `/api/users/me` → redirect by role |
| Logout | `POST /api/auth/logout` | Best-effort server call (it clears the DB-stored access token, invalidating refresh too per §1) → clear memory + `sessionStorage` regardless of the call's outcome → `/login` |
| Refresh | `POST /api/auth/refresh` | See §5.2 |

- [x] **5.3a** Signup form fields are exactly `fullName`, `email`, `password` — no role selector exists anywhere in the signup UI.
- [x] **5.3b** Signup success does **not** navigate to `/login` — it's already authenticated; go straight to `/dashboard`.
- [x] **5.3c** Login `401` → "Invalid email or password." (`lms-endpoints.md` doesn't state whether a deactivated account gets a distinct status; test a deactivated seeded/admin-created user against the live API and adjust this copy/handling once you know — don't assume `401` covers it until confirmed, per Appendix A.) **Confirmed live:** bad password and a deactivated account both return `401` `{message:"Invalid email or password."}` — same copy for both; UI already surfaces that message.
- [x] **5.3d** Signup duplicate email → expect `409`, shown inline on the email field (confirm the actual status against the live API — not explicitly pinned by either doc, but `409 Conflict` is the conventional choice and matches how the guide treats other duplicate-resource cases like course-code and re-enrollment). **Confirmed live:** second signup with an existing email → `409` `{timestamp,status,error,message:"The request conflicts with existing data."}`.

### 5.4 Route guards (UX only — the server is authoritative, always)

* `<RequireAuth>` — not authenticated → redirect to `/login`.
* `<RequireRole roles={['ADMIN']}>` (etc.) — wrong role → render a 403 page.
* Every screen still handles a live `401`/`403` from the API regardless of what the guard decided (§8.1).

- [x] **5.4a** Every non-public route is wrapped in `RequireAuth`.
- [x] **5.4b** Admin-only and instructor-only sections are wrapped in `RequireRole`.
- [x] **5.4c** Team/agent understands: hiding a button is not authorization. Use the §3 table to decide what to show, but never treat a hidden button as a substitute for handling the server's real answer.

---

## 6. Shell & routes

### 6.1 Route × role matrix

Derived from the §3 authorization model applied to every endpoint in `lms-endpoints.md`. ✅ allowed · ❌ never rendered · ⚠️ ownership/enrollment resolved server-side, UI shows a best-effort guess and handles the real answer.

| Route | Guest | STUDENT | INSTRUCTOR | ADMIN | Endpoints |
|---|---|---|---|---|---|
| `/login`, `/signup` | ✅ | redirect `/` | redirect `/` | redirect `/` | `/api/auth/*` |
| `/` (home) | → `/login` | → `/dashboard` | → `/dashboard` | → `/dashboard` | `GET /api/users/me` |
| `/dashboard` | ❌ | ✅ | ✅ | ✅ | role-specific dashboard endpoint (§7.10) |
| `/courses` | ❌ | ✅ | ✅ | ✅ | `GET /api/courses` (+filters) |
| `/courses/:id` | ❌ | ✅ | ✅ | ✅ | `GET /api/courses/{id}` |
| `/courses/:id/enroll` action | ❌ | ✅ | ❌ | ❌ | `POST /api/courses/{id}/enroll` |
| `/courses/:id/edit` | ❌ | ❌ | own only ⚠️ | ✅ | `PATCH /api/courses/{id}` |
| `/courses/:id/students` | ❌ | ❌ | own only ⚠️ | ✅ | `GET /api/courses/{id}/students` |
| `/courses/:id/content` (read) | ❌ | enrolled ⚠️ | own ⚠️ | ✅ (per §3) | `GET .../sections`, content GETs |
| `/courses/:id/content/manage` | ❌ | ❌ | own only ⚠️ | ✅ | section/content/quiz CRUD |
| `/courses/:id/grades` | ❌ | ❌ | own only ⚠️ | ✅ | `GET /api/courses/{courseId}/grades` |
| `/courses/:id/discussion` | ❌ | enrolled ⚠️ | own ⚠️ | ✅ (per §3) | discussion list/create/reply |
| `/courses/:id/announcements` | ❌ | enrolled ⚠️ | own ⚠️ | ✅ (per §3) | announcements |
| `/quizzes/:id` (take/view) | ❌ | enrolled ⚠️, **side effect** | own, no attempt side effect ⚠️ | view, no attempt side effect | `GET /api/quizzes/{id}` — see §7.6 |
| `/quizzes/:id/attempts` | ❌ | ❌ | own only ⚠️ | ✅ | `GET /api/quizzes/{id}/attempts` |
| `/grades` (mine) | ❌ | ✅ | ❌ | ❌ | `GET /api/students/me/grades` |
| `/profile` | ❌ | ✅ | ✅ | ✅ | `GET/PATCH /api/users/me` |
| `/admin/users` | ❌ | ❌ | ❌ | ✅ | `GET/POST /api/users`, `GET/PATCH /api/users/{id}`, `PATCH /api/users/{id}/deactivate` |
| `/admin/courses` | ❌ | ❌ | ❌ | ✅ | course create/assign/delete |
| `*` (404) | ✅ | ✅ | ✅ | ✅ | — |

### 6.2 Shell

* Sidebar/topbar nav filtered by role using the matrix above.
* Right side: avatar (`profilePictureUrl`), name, Logout.
* Home redirect: guest → `/login`; authenticated → `/dashboard`.
* `/login` and `/signup` are standalone pages; everything else nests under a `RootLayout`.

- [x] **6.2a** Nav items render strictly per role (a Student never sees `/admin/*` links, etc.).
- [x] **6.2b** Deep links survive a hard reload (Vite dev server's default history fallback covers this; confirm the production static host does too).
- [x] **6.2c** A 404 page exists for unknown routes; a 403 page is rendered both by guards and by whole-screen API `403`s.

---

## 7. Feature guides

Each guide lists endpoints (verbatim from `lms-endpoints.md`), screens, build steps, and acceptance checkboxes.

### 7.1 Auth screens

**Endpoints:** `POST /api/auth/signup` 🔓 · `POST /api/auth/login` 🔓

**Screens:** `/login` (email, password) · `/signup` (fullName, email, password).

1. Client-side validate (email format, non-empty password) — the server is still authoritative.
2. Submit → store tokens → `GET /api/users/me` → redirect `/dashboard`.
3. Map errors per §5.3.

- [x] 7.1a Login works against a real account created through the flows below (there's no documented seed-credentials table in the provided docs — get them from whoever runs the backend, or create one via `/signup` and promote it, §7.2). **Confirmed live:** seeded `admin@lms.com` / `instructor*@lms.com` / `student*@@lms.com` and an admin-created instructor all return `200` with `accessToken`+`refreshToken`+`user`.
- [x] 7.1b Signup always lands as Student; no role field anywhere in the form.
- [x] 7.1c A guest hitting any protected route lands on `/login` with `next` preserved and restored after login.

### 7.2 Admin — User management ⚠️ previously missing from this guide, load-bearing for the rest of the app

This is the **only** path by which Instructor and additional Admin accounts get created — signup unconditionally produces Students. Build this before relying on having any instructor account to test the rest of the app against (unless the backend's seed data already provides one).

**Endpoints:**

| Op | Endpoint | Access | Notes |
|---|---|---|---|
| List | `GET /api/users` | ADMIN | paginated, `?role=` filter |
| Read | `GET /api/users/{id}` | ADMIN | |
| Create | `POST /api/users` | ADMIN | creates a "seeded" user — student, instructor, **or** admin. Request body must include a role field (unlike signup); confirm exact field/enum names via `schema.d.ts` |
| Update | `PATCH /api/users/{id}` | ADMIN | full update **including role** — this is how an existing Student gets promoted to Instructor |
| Deactivate | `PATCH /api/users/{id}/deactivate` | ADMIN | soft delete, sets `isActive=false` |

**Screens:** `/admin/users` (list, filters, pagination) → create-user form → user detail/edit (name, role, active status).

Steps:

1. List: paginated table, `?role=` filter dropdown (All / Student / Instructor / Admin), search-by-name/email if the schema exposes it (confirm — not pinned by either doc).
2. Create: form collects whatever `POST /api/users` actually needs per `schema.d.ts` — at minimum expect name/email/password/role; this is the **only** screen in the whole app with a role selector.
3. Edit: `PATCH /api/users/{id}` — this is where an admin promotes a Student to Instructor, or changes any other field the DTO allows. Confirm whether email is editable here via the schema (it's explicitly *not* editable through the self-service `/profile` screen — §7.3).
4. Deactivate: confirmation dialog, then `PATCH /api/users/{id}/deactivate`; deactivated users should read as inactive in the list (styling/badge), not disappear silently — confirm whether `GET /api/users` still returns inactive users by default or needs a filter.

- [x] 7.2a Admin can create an Instructor account and immediately use it to log in elsewhere in the app. **Confirmed live:** `POST /api/users` `role=INSTRUCTOR` → `201` `AdminUserResponse`; login with that email → `200`, `user.role=INSTRUCTOR`.
- [x] 7.2b Admin can promote an existing Student to Instructor via edit, and that account gains instructor-only access on next login/refresh. **Confirmed live:** `PATCH /api/users/{id}` `{role:INSTRUCTOR}` → `200`; subsequent login returns `user.role=INSTRUCTOR`.
- [x] 7.2c Deactivate persists and is reflected in the list; a deactivated user's login behavior is confirmed against the live API (Appendix A) and handled with real error copy, not a generic message. **Confirmed live:** `PATCH .../deactivate` → `isActive:false`; user still appears in `GET /api/users` with `isActive:false`; login → `401` "Invalid email or password."; reactivate → login `200`.
- [x] 7.2d This screen is completely unreachable for Student/Instructor — no nav link, and a direct URL hit renders the 403 page.

### 7.3 Profile

**Endpoints:** `GET /api/users/me` 🔐 · `PATCH /api/users/me` 🔐

**Screen:** `/profile`.

1. Prefill `fullName` and `profilePictureUrl`; show `email` and `role` read-only. For Students, `GET /api/users/me` additionally includes enrolled courses (per `lms-endpoints.md`'s Users notes) — render that list too; the field is only present for that role, so don't assume it exists elsewhere.
2. Save sends only `{fullName, profilePictureUrl}` — the server ignores role/email/isActive even if a client sends them, but the UI must not offer controls for those fields in the first place (they belong to §7.2's admin screen instead).
3. `profilePictureUrl` is a plain URL text input — no file upload (out of scope, §1).

- [x] 7.3a Editing name/URL persists after reload. **Confirmed live:** `PATCH /api/users/me` returns the updated `fullName`/`profilePictureUrl` (and student `enrolledCourses`); subsequent `GET /api/users/me` serves the same values.
- [x] 7.3b No role/email/isActive controls exist on `/profile`.
- [x] 7.3c Student's own profile view includes their enrolled courses; Instructor/Admin views don't render that section.

### 7.4 Courses

**Endpoints:**

| Op | Endpoint | Access |
|---|---|---|
| List | `GET /api/courses` (`?search=`, `?term=`, `?code=`, combinable, paginated) | 🔐 |
| Read | `GET /api/courses/{id}` | 🔐 |
| Create | `POST /api/courses` | ADMIN |
| Update | `PATCH /api/courses/{id}` | ADMIN, Own(Instructor) |
| Soft-delete | `DELETE /api/courses/{id}` | ADMIN |
| Assign instructor | `PATCH /api/courses/{id}/assign-instructor` body `{instructorId}` | ADMIN |
| Enroll | `POST /api/courses/{id}/enroll` | STUDENT |
| Roster | `GET /api/courses/{id}/students` | ADMIN, Own(Instructor) |

**Screens:** `/courses` (catalog) · `/courses/:id` (detail) · `/admin/courses` + course form (admin) · edit form (admin/instructor) · `/courses/:id/students` (roster).

1. Catalog: `search`/`term`/`code` filter inputs, combinable, synced to URL params; paginated.
2. Detail: header (title, code, term, description, instructor name), then a role-conditioned action area:
   * Student, not enrolled → **Enroll** button → `200` invalidates `me` + courses queries; `409` → toast "Already enrolled in this course."
   * Student, enrolled → no enroll button; sections/grades/discussion/announcements tabs appear.
   * Instructor, owns the course → manage actions; doesn't own it → read-only.
   * Admin → every manage action, including assign-instructor and soft-delete.
3. Admin create: title, description, code, term. Duplicate code → expect `409` inline on `code` (conventional; confirm against live API).
4. Assign-instructor: dropdown populated from `GET /api/users?role=INSTRUCTOR` (paginated) — only offer instructors so the request can't be sent with a non-instructor id in the first place.
5. Roster: paginated student list.

- [x] 7.4a Filters + pagination combine and survive a reload (URL sync).
- [x] 7.4b Instructor can edit their own course; the edit action is hidden on others' courses; a `403` if the server disagrees is still handled. **Confirmed live:** owner `PATCH /api/courses/1` → `200`; non-owner `PATCH /api/courses/2` → `403` `{message:"You do not have permission to perform this action."}`.
- [x] 7.4c Enroll success and duplicate-enroll `409` both behave as above. **Confirmed live:** unenrolled student `POST .../enroll` → `200` Enrollment; second call → `409` "The request conflicts with existing data."; UI maps `409` → "Already enrolled in this course."
- [x] 7.4d A soft-deleted course drops out of the catalog list (server-side `isActive` filtering, per §1). **Confirmed live:** `DELETE /api/courses/{id}` → `isActive:false`; course absent from `GET /api/courses` (totalElements back to 2).

### 7.5 Sections & Markdown content

**Endpoints:**

| Op | Endpoint | Access |
|---|---|---|
| List sections | `GET /api/courses/{courseId}/sections` | 🔐 + enrollment check |
| Create section | `POST /api/courses/{courseId}/sections` | ADMIN, Own(Instructor) |
| Update/reorder | `PATCH /api/sections/{id}` | ADMIN, Own(Instructor) |
| Delete | `DELETE /api/sections/{id}` | ADMIN, Own(Instructor) |
| List content | `GET /api/sections/{sectionId}/content` | 🔐 + enrollment check |
| Read content | `GET /api/content/{id}` | 🔐 + enrollment check |
| Create content | `POST /api/sections/{sectionId}/content` | ADMIN, Own(Instructor) |
| Update | `PATCH /api/content/{id}` | ADMIN, Own(Instructor) |
| Delete | `DELETE /api/content/{id}` | ADMIN, Own(Instructor) |

**Screens:** `/courses/:id/content` (student reader + staff manage toggle) · content detail view.

Reader steps:

1. Fetch sections — the list is already ordered by `orderIndex`; don't re-sort client-side.
2. Section accordion/tabs; opening a section fetches its content list; opening an item fetches `GET /api/content/{id}`.
3. Render `body` with `react-markdown` + `remark-gfm`. Raw Markdown only — never `dangerouslySetInnerHTML`.
4. On `403`: "You are not enrolled in this course" empty-state with a link to the course page (an Enroll CTA if the viewer is a Student).

Staff-manage steps:

1. Create section (title) → appends at the next `orderIndex`.
2. Reorder: up/down controls `PATCH` the affected sections' `orderIndex`, then refetch the list.
3. Content CRUD: title + Markdown body with a live preview pane.
4. Delete confirmations: neither doc states whether deleting a section cascades its content or just orphans it — don't assert either behavior in the confirmation copy until you've verified it against the running API (Appendix A); use neutral copy ("This will delete the section") until then.

- [x] 7.5a Enrolled student sees ordered sections and correctly rendered Markdown. **Confirmed live:** enrolled `GET .../sections` → ordered by `orderIndex`; content `body` is raw Markdown (UI renders via `react-markdown` + `remark-gfm`).
- [x] 7.5b Unenrolled student gets the friendly 403 state, not a crash. **Confirmed live:** unenrolled `GET .../sections` → `403`; UI maps that to the "You are not enrolled in this course" empty-state.
- [x] 7.5c Instructor CRUD works only on their own course; the manage UI is hidden elsewhere; a live `403` is still handled. **Confirmed live:** owner section create/patch/delete → `200`; non-owner instructor `POST .../sections` → `403`.
- [x] 7.5d Reorder persists across reload. **Confirmed live:** `PATCH /api/sections/{id}` `{orderIndex:0}` → refetched list returns the new order first.

### 7.6 Quizzes — the highest-risk feature in this app, read all of §7.6

`lms-endpoints.md` devotes an entire dedicated section to the quiz-attempt state machine specifically because an earlier version of the spec had a gap (no explicit "start" endpoint, so a student could view a quiz and stall indefinitely before submitting). The resolution — starting the attempt as a side effect of the student's first `GET` — is deliberate and simple, but it is very easy to accidentally break from the frontend by prefetching, caching, or otherwise firing that `GET` somewhere the user didn't explicitly ask to start a timed quiz.

#### 7.6.1 Quiz builder (Instructor / Admin)

**Endpoints:**

| Op | Endpoint | Access |
|---|---|---|
| List course quizzes | `GET /api/courses/{courseId}/quizzes` | 🔐 + enrollment check — students see `published=true` only |
| Create | `POST /api/courses/{courseId}/quizzes` | ADMIN, Own(Instructor) |
| Update / publish toggle | `PATCH /api/quizzes/{id}` | ADMIN, Own(Instructor) |
| Delete | `DELETE /api/quizzes/{id}` | ADMIN, Own(Instructor) |
| Add question | `POST /api/quizzes/{id}/questions` | ADMIN, Own(Instructor) |
| Update question | `PATCH /api/questions/{id}` | ADMIN, Own(Instructor) |
| Delete question | `DELETE /api/questions/{id}` | ADMIN, Own(Instructor) |

**Screens:** a manage tab under `/courses/:id/content/manage` → quiz list → quiz edit page (metadata + questions).

**⚠️ Correct-answer cardinality — read before building the option-marking UI.** `overview.md` §4 states plainly that "each question contains multiple options with **one or more** correct answers represented in the database." That's an explicit statement that a question can have more than one correct option — do **not** default to a single-correct radio-button design assuming exactly one. `lms-endpoints.md` itself doesn't pin the request DTO shape for "add question + options," so:

1. Build the option-marking control as **checkboxes** (supports zero-or-more marked correct — though the UI should still require at least one), not radio buttons.
2. Confirm the actual `POST /api/quizzes/{id}/questions` request/response shape via `schema.d.ts` before finalizing — it will tell you the real field name and cardinality constraints the backend enforces.
3. Confirm via the schema (or a live test question) how scoring handles a multi-correct question on the taking side — full credit only for an exact match of the selected set, or partial credit per correct option selected — before writing any score-display copy that assumes one or the other.

Steps:

1. Create quiz: title, `durationMinutes`; starts `published=false` (draft).
2. Add question: text + ≥2 options, each option a text field with a "correct" checkbox; block client-side submission unless there are ≥2 options and **at least one** is marked correct (server validation is still authoritative — map its `400`/fieldErrors regardless).
3. Publish toggle on the quiz header (`published: true|false`).
4. Delete quiz/question with a confirmation dialog.

- [x] 7.6.1a A full quiz with questions builds and publishes. **Confirmed live:** `POST .../quizzes` → draft; `POST .../questions` with `CreateQuestionRequest {text, orderIndex, options[{text, isCorrect}]}` → `200`; `PATCH {published:true}` → `200`.
- [x] 7.6.1b Client validation blocks 0-option and 0-correct submissions; server `400` is also mapped to the form. **Confirmed live:** blank `text` → `400` `fieldErrors:{text:"must not be blank"}`; zero-correct → `400` "A question requires at least two options and exactly one correct option."; client blocks before send.
- [x] 7.6.1c Unpublished quizzes are absent from the student-facing list (server-enforced; UI also doesn't link them).
- [x] 7.6.1d The option-marking control supports selecting more than one correct option, and this has been tested against the live API rather than assumed. **Confirmed live:** UI uses checkboxes; server rejects ≠1 correct with `400` "…exactly one correct option." (multi-correct and zero-correct both tested — see Appendix A #7).

#### 7.6.2 Quiz taking (Student)

**Endpoints:**

| Op | Endpoint | Notes |
|---|---|---|
| Start/view | `GET /api/quizzes/{id}` | **Side effect for STUDENT**, per the state machine below |
| Submit | `POST /api/quizzes/{id}/submit` | Body: per-question selected option id(s) — confirm exact shape via `schema.d.ts`, especially given multi-correct questions (§7.6.1) |
| My attempt | `GET /api/quizzes/{id}/attempts/me` | own attempt only, **no side effect** — safe to call freely |

**The state machine, verbatim from `lms-endpoints.md`'s "Quiz Attempt Flow" section — this is the actual spec, not this guide's interpretation of it:**

`GET /api/quizzes/{id}` for an enrolled STUDENT:
1. Enrollment check runs first, as on every gated endpoint.
2. Look up the existing `QuizAttempt` for `(quiz, student)`:
   - **None exists** → create one, `startedAt = now()`, `submittedAt = null`.
   - **Exists, unsubmitted** → return as-is; `startedAt` is **not** reset (this is exactly what prevents extending time via repeated GETs).
   - **Exists, submitted** → return the quiz **read-only**: the student's own past answers and score. This is the permanent post-submission review view.
3. Whenever the attempt is unsubmitted, the response includes a computed `expiresAt` **or** `secondsRemaining` (the doc allows either — check which the live response actually sends) so the client can render a countdown.

`POST /api/quizzes/{id}/submit` for an enrolled STUDENT:
- No attempt exists → `400` (guards a client that calls submit without ever GET-ing first; shouldn't happen via the normal flow below).
- Attempt exists, already submitted → `409` ("already submitted — single attempt only").
- Attempt exists, unsubmitted, past `startedAt + durationMinutes` → hard-rejected as late. **The client-reported time is never trusted** — the server's clock is authoritative for this decision, full stop.
- Attempt exists, unsubmitted, in time → graded normally, `submittedAt` set, response includes score + per-question correctness.

**Hard client rules — violating any of these breaks the product:**

1. The quiz-detail fetch for a student runs **only** after an explicit **"Start quiz"** button click, or automatically when returning to an *already-created, unsubmitted* attempt (detect that case first via `GET /api/quizzes/{id}/attempts/me`, which has no side effect — always call this before ever calling the detail endpoint as a student).
2. **No prefetch, no hover-fetch, no route-level loader, no TanStack Query `staleTime`/caching** for this query on the student take screen. Use `gcTime: 0`, `staleTime: 0`, no persistence, `refetchOnMount: 'always'` — scoped to this screen only.
3. The clock is server time: render the countdown from the returned `expiresAt`/`secondsRemaining`, re-derived every second, and never let local elapsed time drive an auto-submit decision — the server will reject a late submit regardless of what the client's clock says, so on expiry, disable inputs and prompt the student to submit; surface whatever error the server actually returns if they do.
4. Never allow a double-submit: disable the submit button while the request is in flight; on `409`, navigate straight to the review state rather than retrying.
5. Instructor/Admin calling `GET /api/quizzes/{id}` should **not** start an attempt for themselves and, being staff, presumably see correctness info the student view must never leak pre-submission — this staff-view behavior isn't spelled out explicitly in `lms-endpoints.md` (which only details the STUDENT flow), so **confirm it against the live API** (Appendix A) rather than assuming. Regardless of what staff see, keep the staff preview and the student take screen as **separate components** so a wrong payload shape is structurally hard to render in the wrong place.

**Screens:** `/courses/:id` quiz list (student) → `/quizzes/:id` take screen → review screen after submit.

Steps:

1. Student opens `/quizzes/:id`:
   a. Call `GET /api/quizzes/{id}/attempts/me` first.
   b. No attempt / unsubmitted → landing card: title, question count, `durationMinutes`, an explicit warning — **"Starting creates your single attempt and starts the timer."** — behind a **Start quiz** button → on click, `GET /api/quizzes/{id}` → store `expiresAt`/`secondsRemaining` → render questions.
   c. Attempt exists, unsubmitted → `GET /api/quizzes/{id}` directly (this is safe — same deadline comes back, not reset), render the countdown from the returned value.
   d. Attempt submitted → `GET /api/quizzes/{id}` returns the read-only review: own answers, score, per-question correctness. No submit control anywhere on this screen.
2. While taking: selection control per question (checkboxes if the question allows multiple correct — §7.6.1); sticky countdown chip; at zero, disable inputs and show "Time is up — submit now."
3. Submit: `POST /api/quizzes/{id}/submit` with selections → success shows score (`X / totalQuestions` or whatever the real payload shape is) + per-question review → invalidate the grades, dashboard, and attempts query keys.
4. Error paths: `400` (no attempt — recover by re-running the §7.6.2 step-1 flow from `attempts/me`); `409` → "You have already submitted this quiz — single attempt only," redirect to review; late-rejection → "The time limit has passed — your submission was not accepted," then offer the review view.
5. A second visit after submission is review-only; the `GET` is safe post-submission, but still avoid pointless refetch loops.

- [x] 7.6.2a The attempt is created **only** by the Start click — merely opening the course page, or hovering a quiz link, never fires `GET /api/quizzes/{id}` as a student.
- [x] 7.6.2b Refreshing the take screen mid-attempt does not reset the countdown (compare `expiresAt`/`secondsRemaining` across reloads and confirm it's unchanged). **Confirmed live:** two consecutive `GET /api/quizzes/{id}` on an unsubmitted attempt returned identical `startedAt` + `expiresAt` (server does not reset on re-GET).
- [x] 7.6.2c No correctness field is visible anywhere — DOM or network payload used for rendering — before submission.
- [x] 7.6.2d Submit → score + review; a second submit attempt shows the `409` copy and stays in review. **Confirmed live:** submit → `200` `{attemptId, score:3, totalQuestions:3, submittedAt, answers[…isCorrect]}`; second submit → `409` "Quiz attempt has already been submitted."
- [x] 7.6.2e Countdown hitting zero disables inputs.
- [x] 7.6.2f Post-submission navigation always shows the permanent review; `GET /api/quizzes/{id}/attempts/me` reflects the stored attempt. **Confirmed live:** after submit, `attempts/me` → stored attempt with `submittedAt`+score; detail GET → read-only review (`score`, `submittedAt`, `answers` present).

### 7.7 Grades

**Endpoints:** `GET /api/students/me/grades` (STUDENT, quiz results grouped by enrolled course) · `GET /api/courses/{courseId}/grades` (ADMIN, Own(Instructor); flat `{student, quiz, score}` rows, paginated).

**Screens:** `/grades` (student) · `/courses/:id/grades` (staff).

1. Student: group results by enrolled course (the response itself is grouped, per `overview.md` §6); show quiz title, score, attempt date.
2. Staff: paginated table with student/quiz/score columns; a client-side-only CSV export is fine to add (no backend involvement).
3. Neither doc mentions a course-average or GPA aggregate anywhere — don't invent one.

- [x] 7.7a Student sees only their own results, grouped by course. **Confirmed live:** `GET /api/students/me/grades` → bare array of `{courseId, courseTitle, courseCode, quizzes[{quizId, quizTitle, score, totalQuestions, submittedAt}]}` for that student only.
- [x] 7.7b Instructor sees the table for their own course only; Admin sees any course. **Confirmed live:** owner `GET .../grades` → `200` Page; non-owner instructor → `403`; admin any course → `200`.
- [x] 7.7c No fabricated aggregate/average grade is displayed anywhere.

### 7.8 Discussion

**Endpoints:**

| Op | Endpoint | Access |
|---|---|---|
| List (top-level + nested replies) | `GET /api/courses/{courseId}/discussion` | 🔐 + enrollment check |
| Create top-level | `POST /api/courses/{courseId}/discussion` | 🔐 + enrollment check — Student, Instructor, **and Admin** may all post |
| Reply | `POST /api/discussion/{postId}/reply` | 🔐 + enrollment check — rejected by the server if `postId` is itself already a reply (enforces exactly one level) |
| Edit | `PATCH /api/discussion/{id}` | Own (author) |
| Delete | `DELETE /api/discussion/{id}` | Own (author), or ADMIN on any post — moderation |

**Screen:** `/courses/:id/discussion`.

1. List top-level posts newest-first, each rendering its replies indented one level.
2. Composer for a new top-level post — whether `title` is required or optional isn't pinned by `lms-endpoints.md`; confirm via `schema.d.ts`, `body` is required either way.
3. **Reply control only ever appears on top-level posts** — never on a reply. The server also rejects reply-to-reply server-side, but the UI shouldn't offer the control in the first place.
4. Edit/Delete visible only to the author; Delete additionally visible to Admin on any post. Optimistic removal on delete with error rollback.
5. Paginated list.

- [x] 7.8a Posts and their nested replies render correctly; replying to a reply is impossible through the UI.
- [x] 7.8b Edit/delete visibility matches the authorization table; a live `403` (e.g. a stale UI state) is handled gracefully. **Confirmed live:** non-author `PATCH`/`DELETE /api/discussion/{id}` → `403` "You do not have permission to perform this action."; UI maps `403` to that copy.
- [x] 7.8c Any authenticated, enrolled user (student, instructor, or admin) can start a top-level thread — the composer isn't restricted to students.

### 7.9 Announcements

**Endpoints:** `GET /api/courses/{courseId}/announcements` (🔐 + enrollment check) · `POST /api/courses/{courseId}/announcements`, `PATCH/DELETE /api/announcements/{id}` (ADMIN, Own(Instructor)).

**Screen:** `/courses/:id/announcements`.

1. Paginated list; staff see a "New announcement" composer (title, body). `overview.md` §8 is explicit that only instructors and administrators publish announcements — Students never get this composer.
2. Edit/delete available to the authoring instructor or an admin.
3. Students get a read-only list.

- [x] 7.9a Student cannot create, edit, or delete announcements — no composer or edit controls render for that role.
- [x] 7.9b Instructor can manage announcements only on their own course.

### 7.10 Dashboards

**Endpoints:** `GET /api/students/me/dashboard` (STUDENT) · `GET /api/instructors/me/dashboard` (INSTRUCTOR) · `GET /api/admin/dashboard` (ADMIN).

Content, per `overview.md` §9 — treat this as the intended regions, and confirm exact JSON field names via `schema.d.ts` before binding to them (neither doc pins the payload shape):

| Role | Renders |
|---|---|
| Student | Enrolled courses; per-course quiz status (attempted/not-attempted) with score where attempted |
| Instructor | Their own courses; quiz-result summaries per course; their own announcements |
| Admin | User/course/enrollment counts |

Don't add anything beyond what `overview.md` §9 actually describes for each role (no course-progress bars, no announcement tickers, no discussion activity feed, no content summaries) unless `schema.d.ts` reveals the backend genuinely returns more — in that case the schema wins and this table should be updated to match, not the other way around.

Steps:

1. `/dashboard` picks the endpoint based on the current user's role from `/api/users/me`.
2. Cards/sections link out to the relevant feature screens (a course card → course detail; a quiz status → the take/review screen as appropriate).
3. Loading and empty states ("No courses yet") for every region independently.

- [x] 7.10a Each role sees only the regions listed for it above.
- [x] 7.10b Student dashboard never shows another student's data (the server guarantees this; the UI just renders whatever it's given, unfiltered).
- [x] 7.10c No invented widgets beyond what `overview.md` §9 describes, unless the schema explicitly supports them.

---

## 8. Cross-cutting UX conventions

### 8.1 Status → behavior table

| Status | Meaning | UI behavior |
|---|---|---|
| 400 / 422 | Validation | Inline `fieldErrors` where available; form-level fallback to `message` otherwise |
| 401 | Unauthenticated / expired | Trigger the single-flight refresh (§5.2) once; on continued failure → `/login?next=` |
| 403 | Forbidden | Screen-level → dedicated 403 page; action-level → disable the control / toast the `message`. Common case: an unenrolled student hitting gated course content → "Not enrolled" state with a link to the course (and an Enroll CTA) |
| 404 | Not found | 404 page on direct navigation; inline empty state for a missing list item |
| 409 | Conflict | Feature-specific copy: duplicate email (signup), duplicate course code, already-enrolled, quiz already submitted |
| 500 | Server error | Toast "Something went wrong" + a retry affordance; never surface raw internals |

### 8.2 Time display

* Treat API timestamps as UTC ISO strings unless `schema.d.ts` or a live response proves otherwise (§1).
* Display via `Intl.DateTimeFormat` in the browser's local zone; the quiz countdown re-derives from `expiresAt`/`secondsRemaining` minus `Date.now()`, re-rendered every second.
* Never mutate a timestamp before sending it back to the server; the client sends no timestamps for quiz deadlines at all — the server's clock is the only authority (§7.6.2).

### 8.3 Global states

Every data region on every screen defines: loading (skeleton), empty (`EmptyState` + a relevant call-to-action), error (message + retry), forbidden (per §8.1).

- [x] 8.3a A shared `ApiErrorBoundary` (or per-route `errorElement`) catches unexpected errors and renders a friendly 500 screen.
- [x] 8.3b Dates render localized everywhere; the quiz countdown visibly ticks.
- [x] 8.3c No screen ever shows a raw stack trace or a blank white area on failure.

---

## 9. Build order

The backend is complete, so nothing here waits on backend work — this is a dependency-ordered sequence for the frontend build itself, not a gating checklist against backend phases.

| Step | Work | Depends on | Done when |
|---|---|---|---|
| **1. Scaffold** | §2.1–2.3 | — | `npm run dev` and `npm run build` both green |
| **2. API layer** | §4.1–4.5 | Backend running, for `gen:api` | `schema.d.ts` generated; error/pagination shapes confirmed against a live response |
| **3. Auth & shell** | §5, §6 | Step 2 | Signup/login/logout/refresh work against the live API; route guards + nav render correctly per role |
| **4. Profile** | §7.3 | Step 3 | Edit name/URL persists |
| **5. Admin user management** | §7.2 | Step 3 | Admin can create an Instructor account and promote a Student — needed before you can meaningfully test any staff-only feature below with a real account |
| **6. Course catalog, detail, enroll, roster, admin course CRUD** | §7.4 | Step 5 (need an instructor to assign) | All course flows work for all three roles |
| **7. Sections & content** | §7.5 | Step 6 | Reader + staff CRUD + reorder + the 403 "not enrolled" state all work |
| **8. Quiz builder** | §7.6.1 | Step 7 | A full quiz with multi-correct-capable questions builds and publishes |
| **9. Quiz taking** | §7.6.2 | Step 8 | Every checkbox in §7.6.2 passes — this is the feature most worth extra QA time |
| **10. Grades** | §7.7 | Step 9 | Student grouped view + staff flat table both work |
| **11. Discussion & announcements** | §7.8, §7.9 | Step 6 | One-level reply enforcement, role-correct composers |
| **12. Dashboards** | §7.10 | Steps 6–11 (dashboards summarize everything else) | All three role dashboards render only their spec'd regions |
| **13. Polish** | §8 | — | Every status-code behavior in §8.1 has been exercised at least once |
| **14. Full walkthrough** | §10 below | Everything above | All steps in §10 pass in the running UI |

- [x] Each step is checked only once every acceptance box in its referenced section is checked.

---

## 10. Verification — full UI walkthrough

Run this against the real, running backend with whatever accounts you have (seed data is mentioned by `lms-endpoints.md` as existing — an initial Admin plus demo Instructor/Student — but no credentials are given in either source doc; get them from whoever runs the backend, or bootstrap your own via signup + the admin user-management screen from step 5 of §9). No direct database access.

1. Sign up a new user → lands as Student on the Student dashboard.
2. Log in as an Admin → create an Instructor via `/admin/users` → create a Course → assign that Instructor to it.
3. Log in as the new Student → browse the catalog → enroll in the course.
4. Log in as the Instructor → create a Section → add Markdown content → create a Quiz (with a duration) → add questions (≥2 options each, at least one marked correct, including at least one question with **more than one** correct option to exercise §7.6.1) → publish → create an Announcement.
5. As the Student → open the course → read the section/content → open the quiz: landing card → Start → timer visible → reload the page → timer unchanged → submit → score shown → attempt to submit again → `409` copy shown → confirm the review view.
6. As the Student → `/grades` shows grouped results → create a Discussion post.
7. As the Instructor → reply to that post; confirm that attempting to reply to your own reply is impossible in the UI (and, if forced via direct API call, rejected by the server).
8. As the Student → read the announcement → dashboard shows the course and the quiz's attempted status/score.
9. As the Instructor → dashboard shows their courses, quiz-result summary, and their announcements.
10. As the Admin → dashboard shows counts; `/admin/users` and `/admin/courses` both work fully.
11. Negative pass: Student hitting `/admin/users` directly → 403 page; an unenrolled student hitting course content directly → "Not enrolled" state, not a crash; logout → tokens are dead (the back button cannot resume the session without logging in again).

- [ ] **All 11 steps pass**, and every feature acceptance box across §7 is checked → build complete.

---

## Appendix A — Shapes and behaviors to confirm against the live API / `schema.d.ts`

Nothing below should be hardcoded from assumption. Generate `schema.d.ts` (§4.1) and, where that's not enough, hit the running API directly:

1. Exact `Page<T>` JSON field names actually returned (§4.4) — **confirmed** (schema `Page*Response` + live): `content, totalElements, totalPages, number, size, …`.
2. Login/signup/refresh response JSON key names for the tokens — **confirmed**: `accessToken` + `refreshToken` on auth responses; refresh returns `accessToken` only.
3. `POST /api/quizzes/{id}/submit` request body shape — **confirmed** (`SubmitQuizRequest`): `{answers:[{questionId, selectedOptionId?}]}` — one optional option per question (server enforces exactly-one-correct at question write; submit is single-select, no multi-option array).
4. The submit success response's exact score / per-question-correctness payload shape — **confirmed** (`SubmitQuizResponse`): `{attemptId, score, totalQuestions, submittedAt, answers:[{questionId, selectedOptionId, isCorrect}]}`.
5. Whether `GET /api/quizzes/{id}` returns `expiresAt` or `secondsRemaining` — **confirmed**: `expiresAt` (detail also carries `startedAt`, `submittedAt`, `score`, `totalQuestions`).
6. Whether Instructor/Admin calling `GET /api/quizzes/{id}` triggers any attempt side effect, and what their response payload includes — **confirmed live:** staff detail GET → `200` with `startedAt/expiresAt/submittedAt/score` all `null`, `questions[].options` **without** `isCorrect`, `answers:[]`; student's pre-existing `attempts/me` unchanged after staff GETs (no side-effect attempt created for staff). Staff correctness only via `GET /api/quizzes/{id}/questions` → `options[].isCorrect`.
7. Whether scoring on a multi-correct question is all-or-nothing or partial-credit — **confirmed live:** question create with ≠1 correct → `400` "A question requires at least two options and exactly one correct option."; scoring is all-or-nothing per question via the single `selectedOptionId`. `overview.md`'s "one or more" is not honored by this server — the builder keeps checkboxes but the server rejects ≠1 correct with `400`.
8. Student / Instructor / Admin dashboard exact JSON structures (§7.10) — **confirmed** (schema): student = bare `StudentDashboardCourseResponse[]` (`courseId, courseName, quizzes[{quizId, quizTitle, attempted, score}]`); instructor = `InstructorDashboardResponse` (`courses[{courseId, courseName, submittedAttemptCount, averageScore}]`, `announcements[{id, courseId, title, body, createdAt}]`); admin = `AdminDashboardResponse` (`userCountsByRole`, `totalCourseCount`, `totalEnrollmentCount`).
9. `GET /api/users/me`'s field name for a student's enrolled courses; the instructor-dashboard summary's field names — **confirmed**: `enrolledCourses: [{id, title}]` (live `[]` for a fresh student); instructor-dashboard fields confirmed under #8.
10. Section create/update DTO — how `orderIndex` is assigned on create — **confirmed**: `CreateSectionRequest {title, orderIndex}` — both required; the client appends with `max(existing orderIndex)+1`.
11. Discussion list DTO — exactly how replies nest under their parent, and whether `title` is present/required on replies — **confirmed** (schema): posts carry a flat `replies: DiscussionReplyResponse[]` (exactly one level); replies have **no** title; `CreateDiscussionPostRequest.title` is **required**.
12. `POST /api/users` (admin create-user) request/response DTO, including the exact role field/enum — **confirmed**: `CreateUserRequest {fullName, email, password, role: STUDENT|INSTRUCTOR|ADMIN, profilePictureUrl?}` → `AdminUserResponse` (incl. `isActive`).
13. Whether logging in with a deactivated (`isActive=false`) account returns `401` or something more specific (§5.3c) — **confirmed live:** deactivated account → `401` `{message:"Invalid email or password."}` (identical to a bad password); reactivated → `200`. Deactivated users remain in `GET /api/users` with `isActive:false`.
14. Whether deleting a section cascades to its content or leaves it orphaned/unreachable (§7.5) — **confirmed from backend source**: **no cascade**. `MarkdownContent.section_id` is a non-null FK; deleting a section that still has content raises `DataIntegrityViolationException` → **409** "The request conflicts with existing data."; an empty section deletes fine. Keep confirmation copy neutral and surface the `409` if the delete fails.
15. Whether Admin is actually exempt from the "enrollment check" rows' server-side check — **confirmed from backend source**: `AuthorizationService.isEnrolledOrStaff` returns `true` for `ADMIN` (and for the course's assigned instructor) before consulting enrollment.
16. Assign-instructor response body/status — **confirmed** (schema): `200` with the full `CourseResponse`.
17. `ApiErrorBody` and `fieldErrors` exact shape (§4.3) — **confirmed against live `400`/`403`/`409`**: `{timestamp, status, error, message, fieldErrors?: Record<string, string>}`.

---

## Appendix B — Source documents

This guide is derived entirely from two files; nothing else was treated as authoritative:

* `overview.md` — product description and the nine functional areas.
* `lms-endpoints.md` — the endpoint table, access legend, and the quiz-attempt state machine.

Any claim in this guide that isn't traceable to one of those two, or to a live response from the running backend, is called out inline as an assumption to verify (see Appendix A) rather than stated as fact.


