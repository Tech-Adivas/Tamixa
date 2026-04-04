# Security Overview

## Implemented Controls

| Area | Control |
|------|---------|
| Auth | BCrypt password hashing, JWT access/refresh tokens |
| Authorization | `@PreAuthorize` by role (ADMIN, PARENT); resource ownership checks |
| Rate limiting | Per-endpoint and story-generation limits |
| Input validation | `@Valid` on DTOs; size limits; theme/language allowlists |
| Webhooks | Signature verification (Stripe, Zoho) before processing |
| CORS | Configurable origins; restrict in production |
| Secrets | From environment; `.env` gitignored (never commit; production: use secrets manager) |
| Content safety | Story moderation (safety score, blocklist) |
| Dev endpoints | `@Profile("dev")`; `SEED_ADMIN_ENABLED` for seed-admin |

## Production Checklist

- [ ] `JWT_SECRET` – Strong secret (≥256 bits); unique per environment
- [ ] `CORS_ALLOWED_ORIGINS` – Explicit origins; no `*` with credentials
- [ ] `SEED_ADMIN_ENABLED=false` – Disable dev seed endpoint
- [ ] `DEV_OTP_CODE` – Unset or remove in production
- [ ] Stripe/Zoho webhook secrets – Valid and kept secret
- [ ] `OPENAI_API_KEY`, `GEMINI_API_KEY` (when using Gemini LLM, Gemini covers, translation, or Veo), `AWS_*` – From secrets manager, not `.env` in CI

## Vulnerability Mitigations

- **SQL injection**: JPA/Hibernate and parameterized queries
- **XSS**: React/Next escape by default; avoid `dangerouslySetInnerHTML`
- **CSRF**: Stateless JWT; no session cookies
- **Secrets in logs**: Rules forbid; PII masked via `PiiMask`; see [docs/backend/LOGGING.md](backend/LOGGING.md)
- **IDOR**: Parent/child/story ownership validated in services

## Reporting

Report security issues privately (e.g., maintainer contact). Do not open public issues for vulnerabilities.
