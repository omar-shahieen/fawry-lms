# University LMS API — Implementation Spec

**Stack:** Spring Boot 4.1.1, Spring Security (JWT), Spring Data JPA, PostgreSQL 18 (Docker), springdoc-openapi 3.1.1 (Swagger)
**Architecture:** Single monolithic Spring Boot app. No microservices, no message queues, no separate auth service. One database. Package-by-feature modules inside one deployable.
**Context:** 4-day take-home technical assessment for a Fawry backend internship. See §7 for scope/timeframe rationale.

---

## 1. Tech Stack & Infra

| Concern | Choice |
|---|---|
| Language/Framework | Java 25 (LTS) + Spring Boot 4.1.1 |
| Auth | Spring Security + JWT (access + refresh tokens) |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL 18 (`postgres:16-alpine`), run via Docker Compose |
| API Docs | springdoc-openapi 3.1.1 → auto Swagger UI at `/swagger-ui.html` |
| Validation | `jakarta.validation` (`@Valid`, `@NotBlank`, etc.) on request DTOs |
| Pagination | Spring Data `Pageable` on all list endpoints (`?page=0&size=20&sort=field,asc`) |
| Build tool | Maven or Gradle (your call — pick whichever you're faster in) |

### docker-compose.yml (Postgres + App)
The whole stack — API and database — runs with a single `docker-compose up`. No manual steps required to reach a working, logged-in-capable state (seed data covers the first login; see §7.7 and the Users section).

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: lms
      POSTGRES_USER: lms_user
      POSTGRES_PASSWORD: lms_pass
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U lms_user -d lms"]
      interval: 5s
      timeout: 5s
      retries: 5

  app:
    build:
      context: .
      dockerfile: Dockerfile
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/lms
      SPRING_DATASOURCE_USERNAME: lms_user
      SPRING_DATASOURCE_PASSWORD: lms_pass
      JWT_SECRET: ${JWT_SECRET:-dev-only-change-me}
      ADMIN_SEED_EMAIL: ${ADMIN_SEED_EMAIL:-admin@lms.com}
      ADMIN_SEED_PASSWORD: ${ADMIN_SEED_PASSWORD:-Admin123!}
    ports:
      - "8080:8080"

volumes:
  pgdata:
```

### Dockerfile (multi-stage — build the app image)
```dockerfile
# Stage 1: build
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: run
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Use `spring.jpa.hibernate.ddl-auto: update` for the MVP (skip Flyway/Liquibase migrations — unnecessary ceremony for 4 days; see §7.1).

---

## 2. Entity Model

Indexing note: Postgres auto-indexes the primary key and any `unique = true` column, but **not** plain foreign key columns. Every `@ManyToOne` FK below needs an explicit `@Index` or it degenerates into a sequential scan on every join/list/ownership check as data grows. The "Index" column marks what each field needs; see §2a for the full rationale and composite indexes.

### User
| Field | Type | Notes | Index |
|---|---|---|---|
| id | UUID | PK | PK (auto) |
| fullName | String | | — |
| email | String | unique, used as login | **Must** — unique index (login lookup on every request) |
| password | String | bcrypt hashed | — |
| role | Enum(STUDENT, INSTRUCTOR, ADMIN) | set server-side only — never client-supplied on signup (see §3 Auth, §7.2) | **Worth** — filtered via `GET /users?role=` |
| profilePictureUrl | String | defaults to a generated DiceBear avatar at creation time if not supplied; editable via `PATCH /users/me` | — |
| isActive | boolean | default true — soft delete flag | **Worth** — filtered on nearly every list query |
| createdAt, updatedAt | Timestamp | stored in UTC (see §7.3) | — |

### Course
| Field | Type | Notes | Index |
|---|---|---|---|
| id | PK | | PK (auto) |
| title | String | | — |
| description | Text | | — |
| code | String | e.g. "CS301" — **globally unique** (see §7 note below) | **Must** — unique index |
| term | String | e.g. "Fall 2026" | **Worth** — combinable filter, see `GET /courses` |
| instructor | FK → User | one instructor per course | **Must** — FK, hit by "my courses" + ownership checks |
| isActive | boolean | soft delete flag | **Worth** — filtered on nearly every list query |
| createdAt, updatedAt | Timestamp | | — |

> **Course code uniqueness:** `code` is globally unique across all terms. Consequence: this schema has no separate "course template vs. offering" concept, so re-running the same course in a later term requires either a new code (e.g. `CS301-F26` vs `CS301-S27`) or updating the existing row's `term` (which loses the previous offering's history). Accepted as a deliberate scope tradeoff for this assessment.

### Enrollment (join entity)
| Field | Type | Notes | Index |
|---|---|---|---|
| id | PK | | PK (auto) |
| student | FK → User | | **Must** — covered by the composite unique index below (as leading column) |
| course | FK → Course | | **Must** — needs its *own* index; the composite below only accelerates lookups starting with `student_id`, but roster queries (`GET /courses/{id}/students`) filter on `course_id` alone |
| enrolledAt | Timestamp | | — |

Unique constraint: `(student_id, course_id)` — declared as `@UniqueConstraint`, Postgres backs it with an index automatically. Also the authoritative guard against a double-enrollment race condition (see §7.4).

### Section
| Field | Type | Notes | Index |
|---|---|---|---|
| id | PK | | PK (auto) |
| course | FK → Course | | **Must** — FK, part of composite (see §2a) |
| title | String | | — |
| orderIndex | Integer | for ordering | Covered by composite `(course_id, orderIndex)` |
| createdAt, updatedAt | Timestamp | | — |

### MarkdownContent
| Field | Type | Notes | Index |
|---|---|---|---|
| id | PK | | PK (auto) |
| section | FK → Section | | **Must** — FK, hit on every content fetch |
| title | String | | — |
| body | Text | raw markdown | — |
| createdAt, updatedAt | Timestamp | | — |

### Quiz
| Field | Type | Notes | Index |
|---|---|---|---|
| id | PK | | PK (auto) |
| course | FK → Course | | **Must** — FK, part of composite (see §2a) |
| title | String | | — |
| durationMinutes | Integer | enforced time limit — see Quiz Attempt Flow below | — |
| published | boolean | draft vs live | **Worth** — filtered together with `course_id` on every student-facing quiz list |
| createdAt, updatedAt | Timestamp | | — |

### Question
| Field | Type | Notes | Index |
|---|---|---|---|
| id | PK | | PK (auto) |
| quiz | FK → Quiz | | **Must** — FK, hit assembling every quiz detail view |
| text | Text | | — |
| orderIndex | Integer | | — |

### QuestionOption
| Field | Type | Notes | Index |
|---|---|---|---|
| id | PK | | PK (auto) |
| question | FK → Question | | **Must** — FK, hit assembling every quiz detail view |
| text | String | | — |
| isCorrect | boolean | | Skip — low cardinality, rarely filtered alone |

### QuizAttempt
| Field | Type | Notes | Index |
|---|---|---|---|
| id | PK | | PK (auto) |
| quiz | FK → Quiz | | **Must** — part of composite unique constraint below |
| student | FK → User | | **Must** — part of composite unique constraint below |
| startedAt | Timestamp | set the moment a student first `GET`s the quiz (see Quiz Attempt Flow) | — |
| submittedAt | Timestamp | nullable until submit | — |
| score | Integer | count of correct answers | — |
| totalQuestions | Integer | snapshot at attempt time | — |

**Unique constraint: `(quiz_id, student_id)`** — single attempt per student per quiz, DB-enforced (see Quiz Attempt Flow and §7.4). This also means the `(quiz_id, student_id)` composite index backs a correctness guarantee, not just a query optimization.

### QuizAnswer
| Field | Type | Notes | Index |
|---|---|---|---|
| id | PK | | PK (auto) |
| attempt | FK → QuizAttempt | | **Must** — FK, hit assembling grading/review results |
| question | FK → Question | | — |
| selectedOption | FK → QuestionOption | | — |
| isCorrect | boolean | computed at submit time | Skip — low cardinality |

### DiscussionPost
| Field | Type | Notes | Index |
|---|---|---|---|
| id | PK | | PK (auto) |
| course | FK → Course | | **Must** — FK, part of composite (see §2a) |
| author | FK → User | | — |
| title | String | nullable for replies | — |
| body | Text | | — |
| parentPost | FK → DiscussionPost, nullable | null = top-level post, set = reply (one level only — enforce in service layer, not schema) | **Must** — FK, hit fetching replies to a post |
| createdAt, updatedAt | Timestamp | | Covered by composite `(course_id, createdAt)` |

### Announcement
| Field | Type | Notes | Index |
|---|---|---|---|
| id | PK | | PK (auto) |
| course | FK → Course | | **Must** — FK, part of composite (see §2a) |
| author | FK → User | Instructor or Admin | — |
| title | String | | — |
| body | Text | | — |
| createdAt, updatedAt | Timestamp | | Covered by composite `(course_id, createdAt)` |

---

## 2a. Indexing Strategy

Declared via `@Table(indexes = {...})` on each entity — this is the right fit here since the spec deliberately skips Flyway/Liquibase and relies on `ddl-auto: update`, which does execute Hibernate's `@Index` definitions as `CREATE INDEX` statements.

### Must-index (foreign keys — join/lookup performance)
Postgres does not auto-index FK columns the way it does the PK. Left unindexed, every join, every "list children of this parent" query, and every ownership/enrollment check becomes a sequential scan as tables grow, and cascading updates/deletes on the parent take a table-level lock while Postgres scans the child table for matches.

- `Course.instructor`
- `Enrollment.student`, `Enrollment.course`
- `Section.course`
- `MarkdownContent.section`
- `Quiz.course`
- `Question.quiz`, `QuestionOption.question`
- `QuizAttempt.quiz`, `QuizAttempt.student`
- `QuizAnswer.attempt`
- `DiscussionPost.course`, `DiscussionPost.parentPost`
- `Announcement.course`

### Must-index (uniqueness / auth path)
- `User.email` — hit on every login; `unique = true` gives you the backing index automatically.
- `Course.code` — globally unique; `unique = true` gives you the backing index automatically.
- `Enrollment(student_id, course_id)` — composite unique constraint, index created automatically.
- `QuizAttempt(quiz_id, student_id)` — composite unique constraint (single attempt per student per quiz), index created automatically.

### Worth-indexing (filter columns hit on nearly every read)
- `User.role` — filtered directly by `GET /api/users?role=`.
- `User.isActive`, `Course.isActive` — soft-delete flag, filtered on almost every list query. Low cardinality alone, so it's most valuable combined into a composite rather than as a standalone index.
- `Quiz.published` — students only see `published = true`; filtered on every student-facing quiz list.
- `Course.term` — filtered directly by `GET /api/courses?term=`.

### Composite indexes (match the actual query shape)
- `Section(course_id, orderIndex)` — sections are always fetched by course and immediately needed in order.
- `Quiz(course_id, published)` — student quiz list filters both together.
- `DiscussionPost(course_id, createdAt)`, `Announcement(course_id, createdAt)` — paginated, course-scoped, newest-first.

### Skip for now
`QuestionOption.isCorrect`, `QuizAnswer.isCorrect`, and standalone timestamp columns — low cardinality or rarely filtered independently; not worth the write overhead at MVP scale. Partial indexes (e.g. `WHERE is_active = true`) would be the ideal shape for the soft-delete filters, but Hibernate's `@Index` annotation can't express a `WHERE` clause — that needs raw SQL, which isn't worth introducing for a 4-day build. Plain composite indexes cover the hot paths well enough here.

---

## 3. Endpoint List

Auth conventions: 🔓 public · 🔐 any authenticated user · role tags = restricted to that role (Admin always implicitly allowed unless noted). "Own" = resource-level ownership check required.

> **Every row's "Access" column is now literally the spec for that method's `@PreAuthorize` annotation — see §4 for the mapping.**

### Auth
| Method | Path | Access | Notes |
|---|---|---|---|
| POST | `/api/auth/signup` | 🔓 | public self-registration. Server hardcodes `role = STUDENT`; any `role` field in the request body is ignored (the `SignupRequest` DTO has no `role` field at all). Body: `fullName`, `email`, `password` (validated). `profilePictureUrl` auto-generated (see Users). Returns access + refresh tokens immediately — no separate login round-trip. |
| POST | `/api/auth/login` | 🔓 | returns access + refresh token |
| POST | `/api/auth/refresh` | 🔓 | exchanges refresh token for new access token |
| POST | `/api/auth/logout` | 🔐 | clears the user's database-stored access token; the JWT filter rejects that access token and refresh rejects the user's refresh token until a new login/signup stores a new access token |

> **Admin & instructor accounts:** signup only ever creates STUDENT accounts. The initial ADMIN account, plus demo INSTRUCTOR/STUDENT accounts, are created by seed data on startup (see §7.7 and Users notes). From there, `POST /api/users` (ADMIN-only, below) is the only way INSTRUCTOR and additional ADMIN accounts are created; `PATCH /api/users/{id}` covers promoting an existing STUDENT to INSTRUCTOR later if needed.

### Users
| Method | Path | Access | Notes |
|---|---|---|---|
| GET | `/api/users/me` | 🔐 | includes enrolled courses if role=STUDENT |
| PATCH | `/api/users/me` | 🔐 | update own name/picture only |
| GET | `/api/users` | ADMIN | paginated, `?role=` filter |
| GET | `/api/users/{id}` | ADMIN | |
| POST | `/api/users` | ADMIN | create seeded user (student/instructor/admin) — the only way to create INSTRUCTOR/ADMIN accounts |
| PATCH | `/api/users/{id}` | ADMIN | full update incl. role |
| PATCH | `/api/users/{id}/deactivate` | ADMIN | soft delete (isActive=false) |

### Courses
| Method | Path | Access | Notes |
|---|---|---|---|
| GET | `/api/courses` | 🔐 | paginated list, all active courses. Filters: `?search=` (matches title, LIKE), `?term=` (exact), `?code=` (exact) — combinable |
| GET | `/api/courses/{id}` | 🔐 | |
| POST | `/api/courses` | ADMIN | |
| PATCH | `/api/courses/{id}` | ADMIN, Own(Instructor) | |
| DELETE | `/api/courses/{id}` | ADMIN | soft delete |
| PATCH | `/api/courses/{id}/assign-instructor` | ADMIN | body: `{instructorId}` |
| POST | `/api/courses/{id}/enroll` | STUDENT | self-enrollment |
| GET | `/api/courses/{id}/students` | ADMIN, Own(Instructor) | roster |

### Sections
| Method | Path | Access | Notes |
|---|---|---|---|
| GET | `/api/courses/{courseId}/sections` | 🔐 + enrollment check | ordered by orderIndex |
| POST | `/api/courses/{courseId}/sections` | ADMIN, Own(Instructor) | |
| PATCH | `/api/sections/{id}` | ADMIN, Own(Instructor) | incl. reordering |
| DELETE | `/api/sections/{id}` | ADMIN, Own(Instructor) | |

### Content (Markdown)
| Method | Path | Access | Notes |
|---|---|---|---|
| GET | `/api/sections/{sectionId}/content` | 🔐 + enrollment check | |
| GET | `/api/content/{id}` | 🔐 + enrollment check | |
| POST | `/api/sections/{sectionId}/content` | ADMIN, Own(Instructor) | |
| PATCH | `/api/content/{id}` | ADMIN, Own(Instructor) | |
| DELETE | `/api/content/{id}` | ADMIN, Own(Instructor) | |

### Quizzes
| Method | Path | Access | Notes |
|---|---|---|---|
| GET | `/api/courses/{courseId}/quizzes` | 🔐 + enrollment check | students see only `published=true` |
| GET | `/api/quizzes/{id}` | 🔐 + enrollment check | includes questions+options, **not** which is correct. **For STUDENT role: see Quiz Attempt Flow below — this call has a side effect.** |
| POST | `/api/courses/{courseId}/quizzes` | ADMIN, Own(Instructor) | |
| PATCH | `/api/quizzes/{id}` | ADMIN, Own(Instructor) | incl. `published` toggle |
| DELETE | `/api/quizzes/{id}` | ADMIN, Own(Instructor) | |
| POST | `/api/quizzes/{id}/questions` | ADMIN, Own(Instructor) | add question + options |
| PATCH | `/api/questions/{id}` | ADMIN, Own(Instructor) | |
| DELETE | `/api/questions/{id}` | ADMIN, Own(Instructor) | |
| POST | `/api/quizzes/{id}/submit` | STUDENT + enrollment check | See Quiz Attempt Flow below. |
| GET | `/api/quizzes/{id}/attempts/me` | STUDENT | own attempt (single attempt only — see below) |
| GET | `/api/quizzes/{id}/attempts` | ADMIN, Own(Instructor) | paginated, all students' attempts |

#### Quiz Attempt Flow (single attempt, timer starts on first view)

This resolves an ambiguity in the original spec: there was no endpoint that explicitly "starts" a quiz attempt, which meant `durationMinutes` could be trivially bypassed (view the quiz, wait indefinitely, submit at leisure). Resolved as follows, deliberately choosing simplicity (no new endpoint) over strict REST purity (no side effects on GET) given the 4-day timeframe — see §7.1.

**`GET /api/quizzes/{id}` — STUDENT, enrolled:**
1. Enrollment check runs as already specified.
2. Look up existing `QuizAttempt` for `(quiz, student)`:
   - **None exists** → create one, `startedAt = now()`, `submittedAt = null`.
   - **Exists, unsubmitted** → return as-is; **do not** reset `startedAt` (prevents extending time via repeated GETs).
   - **Exists, submitted** → return the quiz **read-only**, including the student's past answers and score. This is their permanent post-submission review view.
3. Response includes a computed `expiresAt` (or `secondsRemaining`) field whenever unsubmitted, so the client can render a countdown.

**`POST /api/quizzes/{id}/submit` — STUDENT, enrolled:**
- No attempt exists → 400 (guards a client calling submit without ever GET-ing; shouldn't happen via normal flow).
- Attempt exists, **already submitted** → 409 ("already submitted — single attempt only").
- Attempt exists, unsubmitted, past `startedAt + durationMinutes` → hard-reject as late (client-reported time is never trusted — see §7.4).
- Attempt exists, unsubmitted, in time → grade normally, set `submittedAt`, return score + per-question correctness.

**Single attempt is DB-enforced**, not just app-layer: `QuizAttempt` carries a `@UniqueConstraint(columns = {"quiz_id", "student_id"})` (see §2), so even a race between two near-simultaneous submit calls resolves to one row.

### Grades
| Method | Path | Access | Notes |
|---|---|---|---|
| GET | `/api/students/me/grades` | STUDENT | quiz results grouped by enrolled course |
| GET | `/api/courses/{courseId}/grades` | ADMIN, Own(Instructor) | flat `{student, quiz, score}` rows, paginated |

### Communication — Discussion
| Method | Path | Access | Notes |
|---|---|---|---|
| GET | `/api/courses/{courseId}/discussion` | 🔐 + enrollment check | paginated top-level posts, each with its replies |
| POST | `/api/courses/{courseId}/discussion` | 🔐 + enrollment check | Student/Instructor/**Admin** can post |
| POST | `/api/discussion/{postId}/reply` | 🔐 + enrollment check | reject if `postId` is already a reply (enforce one-level) |
| PATCH | `/api/discussion/{id}` | Own | edit own post/reply |
| DELETE | `/api/discussion/{id}` | Own, ADMIN | Admin can delete any (moderation) |

### Communication — Announcements
| Method | Path | Access | Notes |
|---|---|---|---|
| GET | `/api/courses/{courseId}/announcements` | 🔐 + enrollment check | paginated |
| POST | `/api/courses/{courseId}/announcements` | ADMIN, Own(Instructor) | |
| PATCH | `/api/announcements/{id}` | ADMIN, Own(Instructor) | |
| DELETE | `/api/announcements/{id}` | ADMIN, Own(Instructor) | |

### Dashboards
| Method | Path | Access | Notes |
|---|---|---|---|
| GET | `/api/students/me/dashboard` | STUDENT | combined JSON: enrolled courses + per-course quiz status/best scores |
| GET | `/api/instructors/me/dashboard` | INSTRUCTOR | combined JSON: their courses + quiz result summaries + their announcements |
| GET | `/api/admin/dashboard` | ADMIN | combined JSON: user/course/enrollment counts |

---

## 4. Cross-Cutting Concerns

### 4.1 Authentication & Authorization — secure-by-default

**Design principle:** every endpoint requires authentication, and every endpoint is **ADMIN-only unless a controller method explicitly says otherwise**. Nobody has to remember to lock a new endpoint down — they have to remember to open one up. A forgotten annotation fails *closed* (403), not open.

Two independent layers, both enforced by Spring Security itself — no custom filters or aspects to write or debug:

- **Layer 1 (HTTP layer) — "are you logged in?"** Every path requires a valid JWT except `/api/auth/**`.
- **Layer 2 (method layer) — "are you allowed to call this?"** Every `@RestController` class carries `@PreAuthorize("hasRole('ADMIN')")` as its default. A method is reachable by non-admins only if that specific method carries a more permissive `@PreAuthorize`.

#### Layer 1 — SecurityConfig

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // turns on @PreAuthorize at class/method level
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated() // "logged in" only — @PreAuthorize does the role work
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

JWT roles must map to `GrantedAuthority` as `ROLE_STUDENT` / `ROLE_INSTRUCTOR` / `ROLE_ADMIN` when the `Authentication` is built in `JwtAuthFilter` — that's what lets `hasRole('ADMIN')` work.

`AuthController` is the **one** controller that does not get the class-level `@PreAuthorize` default (signup/login/refresh must work while logged out). Every other controller gets it.

#### Layer 2 — AuthorizationService (unchanged logic, just how it's wired)

```java
@Component("authz")
public class AuthorizationService {

    private final EnrollmentRepository enrollmentRepo;

    public AuthorizationService(EnrollmentRepository enrollmentRepo) {
        this.enrollmentRepo = enrollmentRepo;
    }

    public boolean isOwnerOrAdmin(User user, UUID ownerId) {
        return user.getRole() == Role.ADMIN || user.getId().equals(ownerId);
    }

    public boolean isEnrolledOrStaff(User user, Course course) {
        if (user.getRole() == Role.ADMIN) return true;
        if (user.getId().equals(course.getInstructor().getId())) return true;
        return enrollmentRepo.existsByCourseAndStudent(course, user);
    }
}
```

#### The pattern on a real controller

```java
@RestController
@RequestMapping("/api/courses")
@PreAuthorize("hasRole('ADMIN')") // default for every method below
public class CourseController {

    // POST /api/courses — no annotation needed, inherits class default. ✅ admin-only.

    @GetMapping
    @PreAuthorize("isAuthenticated()") // any logged-in user — overrides the default
    public Page<CourseDto> listCourses(Pageable pageable, @RequestParam(required = false) String search,
                                        @RequestParam(required = false) String term,
                                        @RequestParam(required = false) String code) { ... }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public CourseDto getCourse(@PathVariable UUID id) { ... }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authz.isOwnerOrAdmin(authentication.principal, @courseService.getInstructorId(#id))")
    public CourseDto updateCourse(@PathVariable UUID id, @RequestBody CourseUpdateRequest req) { ... }

    @PostMapping("/{id}/enroll")
    @PreAuthorize("hasRole('STUDENT')")
    public void enroll(@PathVariable UUID id) { ... }

    @GetMapping("/{id}/students")
    @PreAuthorize("hasRole('ADMIN') or @authz.isEnrolledOrStaff(authentication.principal, @courseService.get(#id))")
    public Page<UserDto> roster(@PathVariable UUID id, Pageable pageable) { ... }
}
```

If a check needs data not available as a method parameter (e.g. only an ID, not the full entity), fetch it via a small service lookup as shown in `updateCourse` above — don't force everything through SpEL for purity. Denial throws `AccessDeniedException` automatically, mapped by Spring Security to a 403.

### 4.2 Annotation mapping — every endpoint in §3

Read straight off the "Access" column of each table above.

| Access tag in §3 | `@PreAuthorize` on that method |
|---|---|
| `ADMIN` (or blank) | *(none — inherits class default)* |
| `🔐` | `@PreAuthorize("isAuthenticated()")` |
| `STUDENT` | `@PreAuthorize("hasRole('STUDENT')")` |
| `INSTRUCTOR` | `@PreAuthorize("hasRole('INSTRUCTOR')")` |
| `ADMIN, Own(Instructor)` | `@PreAuthorize("hasRole('ADMIN') or @authz.isOwnerOrAdmin(authentication.principal, <ownerId lookup>)")` |
| `🔐 + enrollment check` | `@PreAuthorize("hasRole('ADMIN') or @authz.isEnrolledOrStaff(authentication.principal, <course lookup>)")` |
| `STUDENT + enrollment check` | `@PreAuthorize("hasRole('STUDENT') and @authz.isEnrolledOrStaff(authentication.principal, <course lookup>)")` |
| `Own` | `@PreAuthorize("@authz.isOwnerOrAdmin(authentication.principal, <authorId lookup>)")` |
| `Own, ADMIN` | same as `Own` — `isOwnerOrAdmin` already lets admin through |
| `🔓` | not behind method security at all — instead added to `permitAll()` in `SecurityConfig` (`/api/auth/**`) |

Practically: walk §3 top to bottom, and for every row that isn't plain `ADMIN`, add the matching line above to that controller method. Everything else needs nothing — it's already locked down.

### 4.3 Standard error response shape
```json
{
  "timestamp": "2026-09-23T10:00:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "You do not have permission to edit this course."
  }

{
  "timestamp": "2026-09-23T10:00:00Z",
  "status": 400,
  "error": "BadRequest",
  "message": "Valdiation Errors",
  "fieldErrors":{
    "email": "email is not formatted correctly"
  }
  }
```
Implement via a single `@RestControllerAdvice` / `@ExceptionHandler` class — one place for `AccessDeniedException`, `EntityNotFoundException`, `MethodArgumentNotValidException` (validation errors), `DataIntegrityViolationException` (unique-constraint races, mapped to 409 — see §7.4), and a generic 500 fallback.

---

## 5. Suggested Package Structure

```
com.fawry.lms
├── config/            # SecurityConfig, JwtConfig, OpenApiConfig
├── security/          # JwtTokenProvider, JwtAuthFilter, UserDetailsServiceImpl, AuthorizationService
├── common/            # GlobalExceptionHandler, ApiErrorResponse
├── seed/              # DataSeeder (CommandLineRunner) — see §7.7
├── auth/              # AuthController, AuthService at root; dtos/
├── user/              # UserController, UserService, UserRepository at root; entities/, dtos/
├── course/            # Course, Enrollment repositories at root; entities/
├── section/           # Section, MarkdownContent repositories at root; entities/
├── quiz/              # Quiz, Question, QuestionOption, QuizAttempt, QuizAnswer repositories at root; entities/
├── grade/             # Grade aggregation service, controller
├── communication/     # DiscussionPost, Announcement repositories at root; entities/
└── dashboard/         # DashboardService, DashboardController
```
One module per bounded concept. Each feature package uses exactly two sub-packages: `entities/` (JPA entities and domain enums) and `dtos/` (request/response types). `*Controller`, `*Service`, and `*Repository` files stay at the feature package root. Cross-cutting packages (`config`, `security`, `common`, `seed`) remain flat.

---

## 6. Explicitly Out of Scope

Frontend, mobile app, video streaming, assignment submission/grading, live classes, private messaging, AI features, advanced analytics, attendance, GPA calculation, payments, email notifications, external calendar integration, plagiarism detection, third-party LMS integrations, PDF content, material comments, quiz question point-weighting, multi-instructor courses, course unenrollment, **password reset / forgot-password flow**, **file upload for profile pictures** (client supplies a URL; see Users notes and §7), **multiple quiz attempts** (single attempt only — see Quiz Attempt Flow in §3).

---

## 7. Assumptions & Constraints

### 7.1 Timeframe & scope
This is a 4-day take-home assessment for a Fawry internship. Priorities, in order: correct core functionality, clean/secure-by-default design, clear documentation of tradeoffs. Full test coverage, production-hardening, and polish beyond what's listed below are explicitly out of scope for this timeframe. Where a decision trades strict correctness/purity for build speed (e.g. the quiz-timer design in §3, using GET's side effect instead of a dedicated `/start` endpoint), that tradeoff is called out explicitly rather than left implicit.

### 7.2 Security & secrets
- JWT signing secret is supplied via environment variable (`JWT_SECRET`), never hardcoded in source or committed `application.yml`. A `.env.example` documents the required variables without real values.
- Access tokens: short-lived (e.g. 15 min) and stored as the user's active session token. Refresh tokens: longer-lived (e.g. 7 days); logout clears the stored access token, which invalidates the access token and prevents refresh until the user authenticates again.
- No rate limiting or login-throttling is implemented. Acceptable for assessment scope; would be a production requirement (e.g. via Bucket4j or an API gateway).
- Public self-signup (§3 Auth) always creates a STUDENT account server-side; the request DTO has no `role` field, so there is no way for a client to escalate role at signup.
- Seeded/default credentials (admin + demo users) are for local evaluation only and documented in the README, not meant to represent production secret-handling practice.

### 7.3 Data & time
- All `Timestamp` fields are stored and compared in **UTC**; no per-user timezone conversion is implemented (display formatting, if any, is a frontend concern — out of scope, no frontend exists).
- Deactivating a `Course` (or `User`) does **not** cascade-deactivate its children (sections, quizzes, etc.) — they simply become unreachable via the now-hidden parent in list views. Kept intentionally simple for this timeframe.

### 7.4 Concurrency
- `Enrollment(student_id, course_id)` and `QuizAttempt(quiz_id, student_id)` unique constraints are relied on as the source of truth for "already enrolled" / "single attempt" — a race between two near-simultaneous requests is resolved by the DB constraint (second request gets a constraint-violation error, mapped to a 409 by the global exception handler), not by application-level locking.
- Quiz submission near the time boundary: the `now() > startedAt + durationMinutes` check happens server-side at submit time, so the authoritative deadline is the server clock, not client-reported time — closes the obvious clock-tampering vector.

### 7.5 Testing
Minimal and scoped to core functionality only — not a primary deliverable given the timeframe, but the two areas with the most non-obvious correctness rules get coverage:
- **Authorization matrix (§4.2):** at least one test per role for each restricted-endpoint pattern (admin-only, own-or-admin, enrolled-or-staff) — confirming both the allowed and denied paths.
- **Quiz-attempt state machine (§3):** start-via-GET creates an attempt, repeated GET doesn't reset `startedAt`, submit after the deadline is rejected, and a second submit attempt is rejected (single-attempt enforcement).

CRUD happy-path tests and everything else are lower priority and may be skipped if time runs out.

### 7.6 Observability
Standard Spring Boot default logging (console, INFO level) is sufficient; no structured logging, request tracing, or external log aggregation is in scope.

### 7.7 Deployment & seed data
- The full stack — Postgres **and** the Spring Boot app — runs via a single `docker-compose up` (see §1). No manual setup steps are required to reach a working, demo-ready state.
- A `DataSeeder` (`CommandLineRunner`) runs on startup and, if the database is empty, inserts:
  - **Users:** 1 ADMIN (from `ADMIN_SEED_EMAIL`/`ADMIN_SEED_PASSWORD` env vars, defaulting to known dev values), 2–3 INSTRUCTORs, 5–8 STUDENTs — all bcrypt-hashed via the same `PasswordEncoder` bean the app uses at runtime, and all given a generated DiceBear avatar URL.
  - **Courses:** 2–3 courses, each assigned to a different seeded instructor.
  - **Enrollments:** students enrolled across courses, some in multiple, so roster/grade queries return real data.
  - **Sections + MarkdownContent:** 2–3 sections per course, each with sample markdown content.
  - **Quizzes + Questions + Options:** at least 1 published quiz per course, 3–5 questions each, correct options marked.
  - **QuizAttempts + QuizAnswers:** a couple of completed attempts pre-seeded, so grade/attempt-history endpoints return non-empty data without the grader needing to submit a quiz first.
  - **DiscussionPosts + Announcements:** a few per course, including at least one reply, to exercise the one-level-reply constraint.
  - Seed insertion respects entity dependency order (users → courses → enrollments/sections → quizzes/questions/options → attempts/answers → discussion/announcements).
- Seeded credentials (all roles) are documented in a "Seeded accounts" table in the README so a grader can log in immediately without reading seed code.
