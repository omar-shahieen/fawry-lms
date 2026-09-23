# Phase 2 — Security foundation

Status legend: `[x]` done, `[ ]` not yet.

- [ ] **2.1 JwtTokenProvider (pure unit, no Spring context)**
  Generate/parse access + refresh tokens, embed `userId` + `role`, configurable expiry.
  **Acceptance:** plain JUnit test — token generated then parsed returns the same `userId`/`role`; an expired token (mint with `-1s` expiry) fails validation; a tampered token fails validation.
  **Commit:** `feat(security): add JwtTokenProvider with unit tests`

- [ ] **2.2 JwtAuthFilter + SecurityConfig (secure-by-default)**
  `anyRequest().authenticated()` except `/api/auth/**` and swagger paths; roles mapped to `ROLE_*` `GrantedAuthority`.
  **Acceptance:** `@SpringBootTest` (MockMvc) — request to a throwaway authenticated-only endpoint with no token → 401; with a valid token → 200.
  **Commit:** `feat(security): add JwtAuthFilter and secure-by-default SecurityConfig`

- [ ] **2.3 AuthorizationService**
  `isOwnerOrAdmin`, `isEnrolledOrStaff`.
  **Acceptance:** unit tests (mocked `EnrollmentRepository`) covering: admin always true; owner true; non-owner non-admin false; enrolled student true; instructor-of-course true; unrelated student false. This is the component-level half of the "authorization matrix" coverage §16 asks for; the other half (one allow + one deny test per restricted-endpoint pattern) is picked up per-endpoint in Phases 4–10, not repeated here.
  **Commit:** `feat(security): add AuthorizationService with unit tests`
