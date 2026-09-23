# AGENTS.md

IMPORT do not assume anything aske if need further information and do not over engineer simple solutions.
## Project

University LMS REST API.

The authoritative functional and technical specification is:

`docs/lms-spec.md` (IMPORTANT this is the source of truth)

acceptaince criteria :
`docs/lms-api-acceptance-criteria.md`

tasks: 

`docs/tasks`

overview of the system:

`docs/overview.md`

Do not invent requirements that are not supported by the specification.

## Stack

* Java
* Spring Boot
* Spring Security + JWT
* Spring Data JPA / Hibernate
* PostgreSQL
* Maven
* Docker Compose
* springdoc-openapi

## Architecture

* Single Spring Boot monolith.
* Package by feature.
* No microservices.
* No message queues.
* No separate authentication service.
* Keep controllers thin.
* Put business rules in services.
* Keep persistence logic in repositories.
* Use DTOs at API boundaries.
* Do not expose JPA entities directly from controllers.

Follow the package structure defined in `docs/lms-spec.md`.

## Security

* Security is deny-by-default.
* Never trust client-supplied roles.
* Never hardcode secrets.
* Never commit `.env` or real credentials.
* JWT secrets come from environment variables.
* Passwords must be hashed with the configured `PasswordEncoder`.
* Never log passwords, JWTs, refresh tokens, or other credentials.
* Do not disable authentication/authorization to make tests pass.
* Follow the authorization matrix in `docs/lms-spec.md`.

## Database

* PostgreSQL is the source of truth.
* Follow the entity model and indexing strategy in `docs/lms-spec.md`.
* Preserve database unique constraints used for concurrency protection.
* Never remove a uniqueness constraint merely to make an operation succeed.
* Use UTC for timestamps.
* Use the indexes specified by the specification.

## API

* Follow the endpoint contract in `docs/lms-spec.md`.
* Use request/response DTOs.
* Validate request DTOs with Jakarta Validation.
* Use `Pageable` for paginated list endpoints where specified.
* Preserve the documented authorization rules.
* Preserve the documented error response shape.
* Do not silently change an endpoint contract.

## Testing

Prioritize tests for:

1. Authorization matrix.
2. Quiz-attempt state machine.
3. Database uniqueness/concurrency behavior.
4. Core happy paths when time permits.

At minimum, run the relevant tests after every behavioral change.

Before declaring the implementation complete, run:

```bash
./mvnw test
./mvnw verify
```

If a command cannot be run, explicitly report that fact.

## Workflow

For every non-trivial task:

1. Read the relevant section of `docs/lms-spec.md`.
2. Inspect the existing implementation before changing it.
3. Identify affected modules and existing patterns.
4. Create a concise implementation plan.
5. Implement the smallest change that satisfies the specification.
6. Run relevant tests.
7. Run formatting/static validation when configured.
8. Review `git diff`.
9. Check for security and authorization regressions.
10. Report what changed and what was verified.

Do not refactor unrelated code.

## Scope

The explicit MVP scope and out-of-scope features are defined in `docs/lms-spec.md`.

Do not implement out-of-scope functionality unless explicitly requested.

## Important Project Decisions

Some decisions are deliberate scope tradeoffs for the 4-day assessment.

Examples include:

* Hibernate `ddl-auto: update` instead of Flyway/Liquibase.
* Single quiz attempt.
* Quiz attempt starts on the first student `GET` of a quiz.
* Client-side logout token discard rather than server-side refresh-token revocation.
* No rate limiting.
* Standard Spring Boot logging.
* No frontend.

Do not "improve" these decisions unless the task explicitly asks for it.

## Skills

Deep procedures live in skills (loaded on demand via the skill tool):

* Work pattern (global): `spec-driven-workflow`, `status-board-planning`, `spec-first-requirements`
* LMS Spring Boot (project): `spring-boot-feature-slice`, `spring-boot-authorization`, `spring-boot-persistence`, `spring-boot-testing`

Prefer loading the matching skill over re-deriving the procedure. This file remains the short rulebook; do not paste skill bodies here. All agents may load any of these skills (`permission.skill` is allow-all).

## Documentation

When implementation changes a documented API contract, architecture decision, security rule, or important behavior, update the relevant documentation.

Do not duplicate the complete specification inside this file.
