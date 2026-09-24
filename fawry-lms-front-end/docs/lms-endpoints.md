
## Endpoint List

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

