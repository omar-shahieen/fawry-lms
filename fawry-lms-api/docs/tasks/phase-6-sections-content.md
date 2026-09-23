# Phase 6 — Sections & content

Status legend: `[x]` done, `[ ]` not yet.

- [x] **6.1 Sections CRUD**
  `GET .../sections`, `POST`, `PATCH` (incl. reordering), `DELETE`.
  **Acceptance:** integration tests — enrollment check enforced on GET (unenrolled student → 403); an instructor `POST`ing a section against a course they don't own → 403 (not just a read-time check); reordering two sections' `orderIndex` reflects in the next GET.
  **Commit:** `feat(section): implement sections CRUD with enrollment-gated reads`

- [ ] **6.2 Markdown content CRUD**
  `GET .../content` (list), `GET /api/content/{id}`, `POST`, `PATCH`, `DELETE`.
  **Acceptance:** integration tests mirroring 6.1's access pattern.
  **Commit:** `feat(content): implement markdown content CRUD`
