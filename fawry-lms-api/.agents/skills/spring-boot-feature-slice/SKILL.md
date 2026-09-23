---
name: spring-boot-feature-slice
description: Adds endpoints or modules to this LMS API using package-by-feature slices (entity, repository, DTO, service, controller) with Jakarta validation, Pageable, and class-level ADMIN default. Use when adding CRUD endpoints, new feature packages, or request/response DTOs.
---

# Spring Boot Feature Slice (LMS)

Add features as vertical slices inside the existing monolith. Copy the nearest completed module’s structure — do not introduce new layering styles.

## Package layout (spec §5)

```
com.fawry.lms
├── config/ security/ common/ seed/     # shared cross-cutting (flat)
└── <feature>/                          # auth, user, course, section, quiz, grade, communication, dashboard
    ├── <Entity>Controller.java         # at feature package root
    ├── <Entity>Service.java            # at feature package root
    ├── <Entity>Repository.java         # at feature package root
    ├── entities/                       # JPA entities and domain enums
    └── dto/                            # request/response DTOs
```

One module per bounded concept. Allowed sub-packages: `entities/` and `dto/` only — controllers, services, and repositories stay at the feature package root.

## Slice rules

| Layer | Rule |
|---|---|
| Controller | Thin: map HTTP ↔ DTOs, `@PreAuthorize`, `Pageable`, delegate to service. No business logic. |
| Service | Business rules, authorization data lookups (owner id, course), transactions where needed. |
| Repository | Spring Data JPA only. Persistence queries live here. |
| DTOs | All API boundaries. Never expose JPA entities from controllers. |
| Validation | `@Valid` + Jakarta constraints on request DTOs (`@NotBlank`, etc.). |
| Pagination | `Pageable` on list endpoints where the spec says paginated; return `Page<Dto>`. |

## Authorization default

Every new `@RestController` except Auth-style public bootstrap controllers:

```java
@RestController
@RequestMapping("/api/...")
@PreAuthorize("hasRole('ADMIN')") // class default — fails closed
public class ...Controller {
```

- Open only the methods the spec’s Access column allows, using the mapping in the `spring-boot-authorization` skill.
- Never rely on “forgot the annotation” to mean public — forgotten methods stay ADMIN-only.

## Contract discipline

- Endpoint path, method, access, and response shape come from `docs/lms-spec.md` §3 (and related sections). Cite the section you followed.
- Errors go through the existing `common` `GlobalExceptionHandler` shape: `{timestamp, status, error, message[, fieldErrors]}`. Do not invent a new envelope.
- Preserve documented authorization rules exactly (STUDENT/INSTRUCTOR/ADMIN, ownership, enrollment).

## Checklist before done

1. Controller thin; service holds rules; no entities in responses.
2. Request DTO validated; response DTO explicit.
3. Class-level `@PreAuthorize` present; method-level override only where spec allows.
4. Lists paginated when spec says so.
5. Integration tests for allow **and** deny paths (see `spring-boot-testing`).
6. Spec section cited in the completion report.
