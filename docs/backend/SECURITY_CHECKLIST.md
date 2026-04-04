# Backend Security Checklist

Quick-reference checklist aligned with [SECURITY.md](../SECURITY.md). Use before each release.

## Production Deployment

- [ ] `JWT_SECRET` – Strong secret (≥256 bits); unique per environment
- [ ] `CORS_ALLOWED_ORIGINS` – Explicit origins; no `*` with credentials
- [ ] `SEED_ADMIN_ENABLED=false` – Disable dev seed endpoint
- [ ] `DEV_OTP_CODE` / `DEV_PASSWORDLESS_CODE` – **Unset in production** (startup fails if set with `prod` profile — `ProductionDevBypassValidator`)
- [ ] Stripe/Zoho webhook secrets – Valid and kept secret
- [ ] If Stripe enabled: `SUBSCRIPTION_WEBHOOK_ENCRYPTION_KEY` set (32-byte Base64 AES) – required in prod so webhook payloads are encrypted at rest (PCI/compliance)
- [ ] `OPENAI_API_KEY`, `GEMINI_API_KEY` (per `AI_LLM_PROVIDER` / cover / Veo), `AWS_*` – From secrets manager, not `.env` in CI

## Completion Steps

1. Run `./scripts/verify-phase1-enterprise.sh` or `./gradlew :backend:test -Ptamixa.backendOnly=true` – ensure tests pass
2. Grep for hardcoded secrets: `rg -i "api_key|password|secret" backend/src --glob '!*Test*'`
3. Verify `.env` is gitignored and `.env.example` has no real values
4. Set `spring.profiles.active=prod` in production
5. Run `SECURITY_CHECKLIST` audit commands below

## Implemented Controls (Verify)

| Area | Control | Location |
|------|---------|----------|
| Auth | BCrypt password hashing | AuthService |
| Auth | JWT access/refresh tokens | AuthController, TokenService |
| Authorization | `@PreAuthorize` by role | Controllers |
| Authorization | Resource ownership checks | StoryService, ChildService |
| Rate limiting | Per-endpoint limits | RateLimitConfig |
| Input validation | `@Valid` on DTOs | DTOs, Controllers |
| Content safety | Story moderation | ModerationService |
| Dev endpoints | `@Profile("dev")` | Dev controllers |
| Prod startup | JWT secret, CORS, dev bypass codes | `JwtSecretValidator`, `CorsOriginValidator`, `ProductionDevBypassValidator` |
| HTTP headers | Clickjacking + referrer policy | `SecurityConfig` — `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin` |

## Compliance & monitoring (recommended)

- [ ] **Penetration test:** Schedule annual pen test (or after major changes); retain report for auditors (see [docs/COMPLIANCE.md](../docs/COMPLIANCE.md) Section 2.4).
- [ ] **Error tracking / APM:** Integrate Sentry (errors) and optionally Datadog (APM/metrics) for production; alert on auth failures, 5xx spikes, webhook signature failures.
- [ ] **Incident response:** Assign incident lead and Compliance/DPO contacts; keep [docs/INCIDENT_RESPONSE.md](../docs/INCIDENT_RESPONSE.md) updated and linked from runbooks.

## Audit Commands

```bash
# No secrets in codebase
rg -i "password|secret|api_key" backend/src --glob '!*Test*'

# Verify @PreAuthorize on protected endpoints
rg "@PreAuthorize" backend/src

# Verify @Valid on request DTOs
rg "@Valid" backend/src
```
