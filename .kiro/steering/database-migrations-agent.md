---
inclusion: manual
---

# Database / Migrations Agent

When adding or changing schema or migrations, follow these rules. Flyway runs from `classpath:db/migration`; JPA uses `ddl-auto: validate` (no auto-DDL).

## Migration Files

- **Location**: `backend/src/main/resources/db/migration/`
- **Naming**: `V{n}__short_description.sql` – version number only, no gaps in sequence for new migrations (next after highest existing V).
- **One logical change per file** – One migration = one feature or fix (e.g. new table, new column, new index). Keeps history clear and rollback reasoning simpler.

## Backward Compatibility

- **Additive first** – Prefer adding columns/tables over changing or dropping. New columns: use `NULL` or a default so existing rows are valid.
- **Drops and renames** – Avoid dropping columns or tables in the same release as code that still references them. Prefer: add new column → deploy code that uses it → later migration to drop old column.
- **Data migrations** – If backfilling or transforming data, do it in a dedicated migration after the schema change. Keep DDL and data changes readable (comments if needed).

## Schema Conventions

- **Tables**: snake_case. Primary key `id BIGSERIAL` (or UUID if specified). Use `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()` (and `updated_at` if needed).
- **Indexes**: Create for foreign keys and common filters (e.g. `parent_id`, `created_at`, `status`). Name: `idx_<table>_<column(s)>`.
- **FKs**: Use `REFERENCES parent(id) ON DELETE CASCADE` (or SET NULL) explicitly. Index foreign key columns.

## Safety

- **No destructive changes without a plan** – Document drops or breaking changes; consider backup/restore or a two-phase deploy (migration + code + cleanup migration).
- **Test migrations** – Run against a copy of prod-like data or use Testcontainers; ensure Flyway runs successfully and app starts with `ddl-auto: validate`.

## Config

- Flyway: `spring.flyway.enabled: true`, `locations: classpath:db/migration`, `baseline-on-migrate: true` (see `application.yml`).
- Never use `ddl-auto: create` or `update` in production; schema is owned by Flyway.
