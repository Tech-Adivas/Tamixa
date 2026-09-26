---
inclusion: always
---

# Security & Vulnerability Prevention

## Secrets & Credentials

1. **Never log secrets** – No passwords, tokens, API keys, or PII in log messages or `data` fields. Mask: `***${last4}`.
2. **Env for secrets** – API keys, JWT secret, DB password come from environment (`.env` / config server), never hardcoded.
3. **`.env` is gitignored** – Never commit `.env`. Use `.env.example` as a template only.

## Input Validation

1. **Validate all inputs** – Use `@Valid`, `@Size`, `@NotBlank` on DTOs. Reject oversized payloads.
2. **Parameterized queries** – Use JPA, JdbcTemplate, or named parameters. Never concatenate user input into SQL.
3. **Allowlists over blocklists** – For themes, languages, statuses: validate against allowed values.

## Authentication & Authorization

1. **@PreAuthorize** – Protect endpoints by role (`hasRole('ADMIN')`, `hasRole('PARENT')`).
2. **Resource ownership** – Verify parent owns child/story before returning or modifying.
3. **Dev endpoints** – `/dev/**` must be `@Profile("dev")` and disabled in production (`SEED_ADMIN_ENABLED=false`).

## Web Security

1. **CSRF** – Disabled for stateless JWT APIs; ensure token in header, not cookie.
2. **CORS** – Restrict origins in production (no `*` when credentials allowed).
3. **Rate limiting** – Applied on auth, story generation, and general endpoints.

## Children's App Specific

- **Content safety** – Story content passes moderation (safety score, blocklist).
- **No PII in logs** – Child names, emails, phones must be masked or omitted.
