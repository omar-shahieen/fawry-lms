---
description: Implement the next unchecked step (or a specific step) of a phase via the full spec-driven workflow
agent: build
---

Phase selector: $ARGUMENTS

Execute one status-board step end-to-end:

1. Load skills: `status-board-planning`, `spec-driven-workflow`. Load Spring Boot skills as needed (`spring-boot-feature-slice`, `spring-boot-authorization`, `spring-boot-persistence`, `spring-boot-testing`).
2. Resolve the target from `$ARGUMENTS`:
   - A phase number like `7` → use `docs/tasks/phase-7-*.md` (match the file by prefix `phase-7-`).
   - A step id like `7.3` → that specific step in the phase file.
   - Empty → ask the user which phase, or infer from unchecked items in `plan.md`.
3. In the phase file, select the first unchecked `- [ ]` step for the phase, or the exact step id given.
4. Read the relevant section of `docs/lms-spec.md` for that step’s behavior. Do not implement from assumption.
5. Inspect existing code and implement the **smallest** change that meets the step’s work description.
6. Turn every **Acceptance:** bullet into (or locate) a passing test. Run the relevant tests; run `./mvnw test` (and `./mvnw verify` if this completes a meaningful unit of work). If a command cannot run, report that.
7. Review `git diff` and check for security/authorization regressions.
8. Do **not** commit unless the user has explicitly told you to commit. If they have, use the step’s prescribed **Commit:** message exactly. Otherwise show the proposed message and wait.
9. When acceptance passes: mark `- [x]` in the phase file **and** the matching line in `plan.md`.
10. Report: step id, what changed, spec section, tests run/skipped, and whether status checkboxes were updated.

Stop after this one step unless the user asks to continue. Do not start the next step automatically.
