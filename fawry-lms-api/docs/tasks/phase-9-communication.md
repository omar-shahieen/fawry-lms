# Phase 9 — Communication

Status legend: `[x]` done, `[ ]` not yet.

- [x] **9.1 Discussion: list + create top-level post**
  **Acceptance:** integration test — enrollment-gated read/write; Student, Instructor, and Admin can each create a top-level post in a course they have access to (Admin posting is not moderation-only); an unenrolled student → 403; list returns top-level posts with their replies nested.
  **Commit:** `feat(discussion): implement discussion list and post creation`

- [ ] **9.2 Discussion: reply + edit + delete**
  **Acceptance:** integration tests — replying to a top-level post succeeds; replying to a reply → rejected (one level only); edit/delete restricted to the author; delete additionally allowed for Admin on any post/reply (moderation) even when not the author.
  **Commit:** `feat(discussion): implement reply, edit, and moderated delete`

- [ ] **9.3 Announcements CRUD**
  **Acceptance:** integration test — same enrollment-gated read, admin/owning-instructor write pattern as sections; list newest-first; non-existent announcement → 404.
  **Commit:** `feat(announcement): implement announcements CRUD`
