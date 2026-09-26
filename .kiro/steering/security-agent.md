---
inclusion: manual
---

# Security Agent

When performing security review or audit, verify the following. See also `.cursor/rules/security-vulnerabilities.mdc` for day-to-day rules.

## Secrets & Credentials

- No hardcoded API keys, JWT secrets, or DB passwords. All from environment or config.
- No secrets or PII in logs (mask with `***${last4}` or omit).
- `.env` is gitignored; `.env.example` has placeholders only, no real values.

## Input & Injection

- All DTOs use `@Valid` and constraints (`@Size`, `@NotBlank`, allowlists). Reject oversized payloads.
- No string concatenation into SQL; use JPA, JdbcTemplate, or named parameters.
- Allowlists for enums (themes, languages, statuses); no raw user input in queries or commands.

## Authentication & Authorization

- Protected endpoints use `@PreAuthorize` with correct roles (`ADMIN`, `PARENT`).
- Resource ownership: verify parent owns child/story before read/update/delete.
- `/dev/**` and seed endpoints are `@Profile("dev")` and disabled when `SEED_ADMIN_ENABLED=false` in production.

## Web & API Security

- CORS: restrict origins in production; no `*` when credentials are used.
- Rate limiting applied on auth, story generation, and general API.
- Error responses do not expose stack traces or internal paths in production.

## Children's App Specific

- Story content: moderation/safety score and blocklist applied before publish.
- No PII (child names, emails, phones) in logs or client-visible payloads beyond what is strictly required.

## When Flagging

- State the rule violated and the risk (e.g. “Logging token → credential leak”).
- Suggest a concrete fix (e.g. “Use env var `JWT_SECRET` and remove from code”).

## MCP and agent tools

- Follow [docs/MCP_POLICY.md](../../docs/MCP_POLICY.md): approved servers, least privilege, no Act-mode tools without explicit approval.
- For MCP or coding-agent integrations, flag **scope expansion**, **secret exposure**, and **missing audit** trails.
