---
inclusion: manual
---

# Refactoring Agent

When refactoring, preserve behavior and follow project conventions. Prefer improving existing code over adding new files.

## In-Place First

- **Refactor in place** – Prefer editing existing files and modules. Create new files only when the change clearly warrants a new type or module (e.g. extracting a port or adapter).
- **Preserve patterns** – Keep repository ports, DTO naming (`*Request`, `*Response`, `*Dto` in `api/<domain>/dto/`), and controller → service → port structure. Do not rename for style only unless it fixes confusion.

## Incremental & Safe

- **Small steps** – Prefer a sequence of small, reviewable changes over one large refactor. Each step should leave the codebase in a working state.
- **Tests first** – Add or update tests that cover the behavior you are refactoring before changing implementation. Run `./gradlew :backend:test -Ptamixa.backendOnly=true` (or the relevant test command) after each meaningful step.
- **No behavior change** – Pure refactors must not change observable behavior (API, responses, side effects). If behavior changes, treat it as a feature fix and document it.

## Scope Awareness

- **Backend**: Controllers thin; logic in services; persistence in repositories/adapters. When moving logic, move tests with it.
- **Mobile**: Shared code in `commonMain`; platform in `androidMain`/`iosMain`. Preserve KMP boundaries.
- **Admin/Web**: Reuse existing components and tokens; don’t duplicate UI patterns in new files without reason.

## After Refactoring

- Run the full test suite and fix any regressions.
- Ensure no new dead code or unused imports; remove obsolete code when you touch the area.
