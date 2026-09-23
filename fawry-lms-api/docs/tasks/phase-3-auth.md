# Phase 3 — Auth endpoints

Status legend: `[x]` done, `[ ]` not yet.

- [x] **3.1 POST /api/auth/signup**
  Public self-registration. `SignupRequest` DTO has `fullName`/`email`/`password` only — **no `role` field exists on the DTO**, so anything sent in a `role` key is simply unbound/ignored, not validated or rejected. Created user is always `STUDENT`, `isActive=true`, and gets an auto-generated `profilePictureUrl` (DiceBear avatar) since none can be supplied at signup. Returns access + refresh tokens immediately — no separate login call needed.
  **Acceptance:** integration test — valid signup → 201, response includes both tokens and the created user is `STUDENT`; posting a body that also includes `"role":"ADMIN"` → still creates a `STUDENT` (no error, field is just ignored); duplicate email → 409; missing/invalid field (bad email format, blank password) → 400/422; created user's `profilePictureUrl` is non-null.
  **Commit:** `feat(auth): implement POST /api/auth/signup`

- [ ] **3.2 POST /api/auth/login**
  Validates email/password against bcrypt hash, issues access+refresh tokens.
  **Acceptance:** integration test — seed a user, correct credentials → 200 with both tokens; wrong password → 401; a deactivated (`isActive=false`) user → 401 even with correct credentials; response never includes the password field.
  **Commit:** `feat(auth): implement POST /api/auth/login`

- [x] **3.3 POST /api/auth/refresh**
  **Acceptance:** integration test — valid refresh token → 200 new access token; expired/invalid refresh token → 401.
  **Commit:** `feat(auth): implement POST /api/auth/refresh`

- [x] **3.4 POST /api/auth/logout**
  Authenticated logout clears the user's database-stored access token and returns 204. The JWT filter rejects the cleared token, and refresh rejects the user's refresh token while no access token is stored.
  **Acceptance:** integration test — authenticated call → 204; stored access token becomes null; old access and refresh tokens are rejected.
  **Commit:** `feat(auth): implement POST /api/auth/logout`

- [ ] **3.5 GlobalExceptionHandler: auth + validation cases**
  Done as part of step 0.4's commit (see note there) — pulled forward because login needed it. No separate commit.
