# Prepare release

**Goal:** Assemble release notes, verification steps, and rollback draft without performing production deployment.

## Inputs

- Target branch (e.g. `develop` → `main`)
- Diff or merged PR list since last release
- [docs/runbooks/](../../docs/runbooks/) and Flyway migrations in `backend/src/main/resources/db/migration/`

## Produce

1. **Summary** — user-visible changes vs internal-only
2. **Config / env** — new or changed variables (document in `.env.example` / `admin/.env.example`)
3. **Database** — migrations order, backward compatibility, rollback considerations
4. **Smoke tests** — API, admin, web, mobile sanity checks
5. **Rollback plan** — revert commit, migration strategy, feature flags

## Rules

- **Only CI/CD and authorized humans** deploy to production — do not instruct agents to deploy directly.
- Confirm **no** `@Profile("dev")` or seed endpoints enabled in production config.
- Align with [.cursor/rules/delivery-agent.mdc](../../.cursor/rules/delivery-agent.mdc).
