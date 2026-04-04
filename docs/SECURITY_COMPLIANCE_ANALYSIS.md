# Security & Compliance Analysis

**Date:** March 2026  
**Scope:** Tamixa backend, admin, mobile, web — alignment with project security rules and `docs/COMPLIANCE.md`.

---

## Executive Summary

The application **largely meets** the project’s security and compliance expectations. Auth is role-based with JWT; inputs are validated; CORS is enforced in production; rate limiting and Stripe webhook verification are in place; and PII masking is used in auth/OTP logs. **Gaps** are mainly: PII in SMS/audit logs, missing size limits on some DTOs, and a few documentation/operational items from COMPLIANCE.md.

---

## 1. Secrets & Credentials

| Rule | Status | Evidence |
|------|--------|----------|
| No hardcoded API keys / JWT secret / DB password | ✅ | Keys and secrets come from `AppProperties` / env (e.g. `OPENAI_API_KEY`, `GEMINI_API_KEY`, `JWT_SECRET`, Stripe, Twilio). |
| `.env` gitignored | ✅ | `.gitignore` includes `.env`, `.env.local`, `*.env`. |
| `.env.example` template only | ✅ | Placeholders only; no real values. |

**Recommendation:** Ensure production injects secrets via a secrets manager or CI/CD; avoid committing any env file with real values.

---

## 2. Input Validation

| Rule | Status | Evidence |
|------|--------|----------|
| DTOs use `@Valid` and constraints | ✅ | Auth: `RegisterRequest` (`@NotBlank`, `@Email`, `@Size(min=8,max=100)` for password); `LoginRequest` (`@NotBlank`). Controllers use `@Valid @RequestBody`. |
| Parameterized queries / no raw SQL concatenation | ✅ | JPA repositories and named parameters used; no string concatenation into SQL. |
| Allowlists for enums (themes, languages, statuses) | ✅ | Story/theme/language validated in services and config. |

**Gaps:**

- **LoginRequest** has no `@Size(max=...)` on `email` or `password`. Oversized payloads could be used for DoS. **Fix:** Add e.g. `@Size(max=255)` on email and `@Size(max=500)` on password (or align with `RegisterRequest` max).
- **Oversized payloads:** Confirm any other public DTOs (e.g. story create, child create) have reasonable `@Size`/length limits on string fields to reject huge bodies.

---

## 3. Authentication & Authorization

| Rule | Status | Evidence |
|------|--------|----------|
| Protected endpoints use `@PreAuthorize` | ✅ | Parent APIs: `@PreAuthorize("hasRole('PARENT')")`. Admin: `@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN',...)")` and `@adminAuth.hasPermission('...')`. |
| Resource ownership | ✅ | `StoryService`: `if (story.parentId != parent.id) throw StoryAccessDeniedException(storyId)`. Subscription cancel: `current.parentId != parentId`. Cloned voice: `findByIdAndParentId(profileId, parentId)`. Profile/children loaded by `parent.id` from JWT. |
| Dev endpoints `@Profile("dev")` and gated | ✅ | `DevSeedController`, `DevVerifyConnectionsController` are `@Profile("dev")`. Seed also checks `SEED_ADMIN_ENABLED=true`. |
| JWT in header, stateless, CSRF disabled | ✅ | `SecurityConfig`: session stateless; CSRF disabled; JWT via `JwtAuthenticationFilter`. |

**Note:** `SecurityConfig` permits `$v1/dev/**` without auth; in production the dev controllers are not loaded (`@Profile("dev")`), so those paths are not active. No change required if prod does not use `dev` profile.

---

## 4. Web & API Security

| Rule | Status | Evidence |
|------|--------|----------|
| CORS restricted in production | ✅ | `CorsOriginValidator` (`@Profile("prod")`) fails startup if `CORS_ALLOWED_ORIGINS` is unset or `*`. `SecurityConfig` uses explicit origins when set; dev fallback is localhost. |
| Rate limiting | ✅ | `RateLimitingFilter` (general) and `StoryGenerationRateLimitFilter`; configurable per tier (free/paid/suspicious). `X-RateLimit-Limit` / `X-RateLimit-Remaining` exposed. |
| Error responses do not expose stack traces | ✅ | `GlobalExceptionHandler` returns `ErrorResponse` with message only; generic catch-all uses `e.message?.take(200)`. Full stack is logged server-side only. |

---

## 5. Children's App & Content Safety

| Rule | Status | Evidence |
|------|--------|----------|
| Story content moderation / safety | ✅ | `SafetyValidatorServiceImpl`, `StoryModerationService`, `StorySafetyMiddleware`; blocklist and safety score used before publish. |
| No PII in logs (auth/OTP) | ✅ | Auth: `PiiMask.maskEmail(request.email)` in AuthController. OTP: `maskPhone(request.phone)` in AuthController. |

**Gaps:**

- **TwilioSmsSender** logs:
  - When Twilio from-number is not set: `"To=$toPhone | Code=$code"` — **full phone and OTP in log (PII + secret).**
  - On success: `"OTP SMS sent to $toPhone"` — **full phone number in log (PII).**
  - On failure: `"Failed to send OTP SMS to $toPhone"` — **full phone number in log (PII).**  
  **Fix:** Use `PiiMask.maskPhone(toPhone)` for any log message; never log the OTP code (redact or omit).

- **Admin audit details:** `recordAdminAction(..., details)` persists strings such as `"Deleted parent ${parent.email}"` or `"Updated parent ${saved.email}"` to `AdminAuditEntity`. COMPLIANCE.md data inventory says “Audit logs (email masked)”.  
  **Recommendation:** Either mask email in audit details (e.g. `PiiMask.maskEmail`) or document that admin audit intentionally stores identifiers for accountability and restrict access to audit data.

---

## 6. Payments & Webhooks

| Rule | Status | Evidence |
|------|--------|----------|
| Stripe webhook signature verification | ✅ | `WebhookController` uses `WebhookSignatureVerifier` with `Stripe-Signature`; invalid signature leads to 400. |
| Webhook secret from config | ✅ | Secret from app config / env, not hardcoded. |

---

## 7. Compliance Mapping (DPDP, COPPA, GDPR, SOC 2)

Aligned with `docs/COMPLIANCE.md`:

- **Consent:** Registration and passwordless require Terms/Privacy/Parental attestation; consent stored. Per-child consent and data minimization are documented.
- **Data subject rights:** Data deletion workflow; data export via `DataExportController` and S3.
- **Retention:** Documented in COMPLIANCE.md (e.g. stories 24 months, audit 90 days / 12 months).
- **Encryption:** TLS in transit; AES-256 for voice at rest; no card data stored (Stripe).
- **Vendor (OpenAI):** Minimal data sent (age, language, theme, first name); no persistent storage at OpenAI per DPA; moderation usage documented.

**Recommendations from COMPLIANCE.md already reflected above:**  
PCI scope statement, webhook payload encryption key in prod, penetration testing, and security monitoring (e.g. Sentry/Datadog) remain operational/documentation items.

---

## 8. Summary of Required / Recommended Fixes

| Priority | Item | Action |
|----------|------|--------|
| **High** | PII/secret in TwilioSmsSender logs | Use `PiiMask.maskPhone(toPhone)` for all log output; never log OTP code. |
| **Medium** | LoginRequest size limits | Add `@Size(max=255)` for email and `@Size(max=500)` (or similar) for password. |
| **Medium** | Admin audit details contain unmasked email | Mask in details or document exception and restrict access to audit data. |
| **Low** | Other DTOs | Review public DTOs for max length on string fields to prevent oversized payloads. |

---

## 9. Conclusion

The app meets the main security and compliance goals: secrets from env, validation and allowlists, role-based access and resource ownership, CORS and rate limiting, Stripe webhook verification, content safety, and PII masking in auth/OTP logs. Addressing the **TwilioSmsSender** logging and **LoginRequest** size limits, and clarifying or tightening **admin audit** PII, will bring it fully in line with the project’s security and compliance rules and with `docs/COMPLIANCE.md`.
