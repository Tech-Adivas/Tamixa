---
inclusion: fileMatch
fileMatchPattern: ['backend/**/*.kt']
---

# Backend Kotlin Conventions

## Structure

- **Controllers** – Thin: validate, delegate to service, return ResponseEntity.
- **Services** – Business logic; inject ports, not adapters.
- **Adapters** – Implement ports; `@ConditionalOnProperty` for optional features.
- **DTOs** – `data class` with validation; one per file under `api/<domain>/dto/`. No inline DTOs in controllers.

## Security

- Use `@PreAuthorize` on admin endpoints; verify resource ownership in services.
- Dev-only: `@Profile("dev")` + guard (e.g. `SEED_ADMIN_ENABLED`).
- Never log request/response bodies containing tokens or passwords.

## Persistence

- JPA for domain; use `@Transactional` on write operations.
- Migrations in `db/migration/`; never use `ddl-auto: create`.
- Prefer `findByX` and `Page`; avoid raw SQL unless necessary.

## Async

- `@Async` for long-running work (pipelines, webhooks); return immediately.
- Ensure async methods don't run in same thread as caller (separate executor).
