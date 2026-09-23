---
name: spring-boot-authorization
description: Maps the LMS authorization matrix to Spring Security — SecurityConfig, JWT role authorities, @PreAuthorize patterns, and the authz bean for ownership and enrollment checks. Use when touching SecurityConfig, JWT roles, @PreAuthorize, ownership checks, or enrollment checks.
---

# Spring Boot Authorization (LMS)

Two layers, both enforced by Spring Security. Deny-by-default. Never trust client-supplied roles.

## Layer 1 — HTTP (`SecurityConfig`)

- CSRF disabled, stateless sessions.
- `/api/auth/**` and swagger (`/swagger-ui/**`, `/v3/api-docs/**`) → `permitAll`.
- `.anyRequest().authenticated()` — login only; method security does role work.
- JWT filter before `UsernamePasswordAuthenticationFilter`.
- `@EnableMethodSecurity` on.
- Passwords via the configured `PasswordEncoder` bean (`BCrypt`).

`AuthController` is the **only** controller without the class-level ADMIN default (signup/login/refresh must work logged out).

## JWT → authorities

Map roles in the filter when building `Authentication` as `ROLE_STUDENT` / `ROLE_INSTRUCTOR` / `ROLE_ADMIN` so `hasRole('ADMIN')` works.

## Layer 2 — method security

Class default on every other controller:

```java
@PreAuthorize("hasRole('ADMIN')")
```

### Access column → annotation (spec §4.2)

| Spec Access | Annotation |
|---|---|
| `ADMIN` (or blank) | *(none — inherits class default)* |
| 🔐 any authenticated | `@PreAuthorize("isAuthenticated()")` |
| `STUDENT` | `@PreAuthorize("hasRole('STUDENT')")` |
| `INSTRUCTOR` | `@PreAuthorize("hasRole('INSTRUCTOR')")` |
| `ADMIN, Own(Instructor)` | `@PreAuthorize("hasRole('ADMIN') or @authz.isOwnerOrAdmin(authentication.principal, <ownerId lookup>)")` |
| 🔐 + enrollment check | `@PreAuthorize("hasRole('ADMIN') or @authz.isEnrolledOrStaff(authentication.principal, <course lookup>)")` |
| `STUDENT + enrollment check` | `@PreAuthorize("hasRole('STUDENT') and @authz.isEnrolledOrStaff(authentication.principal, <course lookup>)")` |
| `Own` | `@PreAuthorize("@authz.isOwnerOrAdmin(authentication.principal, <authorId lookup>)")` |
| `Own, ADMIN` | same as `Own` — `isOwnerOrAdmin` already admits admin |
| 🔓 public | not method security — `permitAll` in `SecurityConfig` |

Walk spec §3 top to bottom: every non-plain-ADMIN row gets the matching method annotation; everything else stays class-default.

## `authz` bean

```java
@Component("authz")
public class AuthorizationService {
    boolean isOwnerOrAdmin(User user, UUID ownerId);      // ADMIN or id match
    boolean isEnrolledOrStaff(User user, Course course);  // ADMIN, course instructor, or enrollment exists
}
```

When SpEL only has an ID, fetch via a small service lookup — do not force full entities into SpEL for purity. Denials throw `AccessDeniedException` → 403 via the global handler.

## Hard rules

- Never trust client-supplied roles (signup DTO has no `role` field; server sets STUDENT).
- Never hardcode secrets; JWT secret from environment only.
- Never disable or open security to make tests pass.
- Never log passwords, JWTs, or refresh tokens.
- New endpoints fail **closed** unless explicitly opened per the matrix.
- Preserve unique constraints and ownership checks — no “skip check for dev” paths.

## Tests

Cover allow **and** deny for each pattern you touch (admin-only, own-or-admin, enrolled-or-staff) using `spring-boot-testing`.
