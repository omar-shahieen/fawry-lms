# Phase 0 — Project skeleton

Status legend: `[x]` done, `[ ]` not yet.

- [x] **0.1 Initialize Spring Boot project**
  Maven project with `spring-boot-starter-web`, `spring-boot-starter-actuator`.
  **Acceptance:** `mvn spring-boot:run` starts; `GET /actuator/health` → 200 `{"status":"UP"}`.
  **Commit:** `chore: initialize Spring Boot project skeleton`

- [x] **0.2 Add Postgres via Docker Compose**
  `docker-compose.yml` (postgres:16-alpine), `application.yml` datasource pointing at it, `spring-boot-starter-data-jpa` + `postgresql` driver added.
  **Acceptance:** `docker compose up -d` → `docker compose ps` shows healthy; `mvn spring-boot:run` logs a successful Hibernate/JDBC connection, no errors.
  **Commit:** `chore: add Postgres via docker-compose and configure datasource`

- [x] **0.3 Add springdoc-openapi**
  **Acceptance:** `GET /swagger-ui.html` → 200; `GET /v3/api-docs` → 200 valid JSON.
  **Commit:** `chore: add springdoc-openapi swagger UI`

- [x] **0.4 Add global exception handler + standard error shape**
  `@RestControllerAdvice` with a generic 500 fallback returning the `{timestamp, status, error, message}` shape from the spec. Built out further than originally scoped: since step 3.2 (login) needed a 401 mapping to exist to be testable at all, `BadCredentialsException`→401, `AccessDeniedException`→403, `EntityNotFoundException`→404, `DataIntegrityViolationException`→409, and `MethodArgumentNotValidException`→400 (with `fieldErrors`) were all added in this same commit rather than split across 0.4/3.5. **This means step 3.5 below is already done** — nothing left to do there except add tests once the exceptions it covers are actually reachable from real endpoints.
  **Acceptance:** integration test hitting a throwaway endpoint that throws `RuntimeException` gets back the JSON shape with `status: 500`.
  **Commit:** `feat(common): add global exception handler and standard error response`
