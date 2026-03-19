# Security & Compliance Audit Report

**Classification:** Internal — Security & Compliance  
**Product:** Tamixa (AI storytelling app for children)  
**Audit type:** Security and compliance review (auditor perspective)  
**Date:** March 2026  
**Scope:** Backend (Kotlin/Spring), Admin (Next.js), Mobile (KMP), Web (React); alignment with DPDP, COPPA, GDPR, PCI, and SOC 2 readiness.

---

## 1. Executive Summary

The application implements solid security foundations: role-based access, JWT authentication, input validation and sanitization, CORS enforcement in production, rate limiting, Stripe webhook signature verification, content moderation, and PII masking in most logs. Several **high** and **medium** findings require remediation; **low** and **informational** items should be addressed for full compliance and defence-in-depth.

| Severity | Count | Summary |
|----------|--------|--------|
| **Critical** | 0 | — |
| **High** | 2 | iOS token storage plaintext; admin audit stores unmasked PII. |
| **Medium** | 3 | RefreshTokenRequest unbounded; admin rate-limit bypass; Swagger/actuator public in prod. |
| **Low** | 4 | Dev credentials in code; X-Forwarded-For trust; webhook encryption key optional. |
| **Informational** | 4 | Admin JWT in localStorage; doc/ops items. |

---

## 2. Findings

### 2.1 HIGH — iOS tokens stored in plaintext

**Rule / standard:** Secure storage of credentials (OWASP, SOC 2 CC6.6, GDPR/DPDP security).

**Evidence:**  
`mobile/composeApp/src/iosMain/kotlin/com/tamixa/security/TokenStorage.ios.kt` stores access and refresh tokens in `NSUserDefaults`, which is not encrypted. The file explicitly states: *"SECURITY TODO: Migrate to Keychain (Security framework) so tokens are encrypted at rest."*

**Risk:** On a lost or compromised device, or via backup extraction, JWTs can be recovered and used to impersonate the user. Child and parent data could be accessed.

**Remediation:**  
- Store tokens in iOS Keychain (e.g. `kSecClassGenericPassword`, `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` or equivalent).  
- Use Swift/Obj-C Keychain APIs via KMP cinterop or a shared Swift implementation, as noted in `docs/mobile/MOBILE_APP_AUDIT.md`.  
- Retire NSUserDefaults for any secret or token.

---

### 2.2 HIGH — Admin audit log stores unmasked PII (email)

**Rule / standard:** No PII in logs (project rules, COMPLIANCE.md §14.2, GDPR/DPDP minimisation).

**Evidence:**  
`AdminService.recordAdminAction(..., details)` persists a free-form `details` string to `AdminAuditEntity`. Call sites pass strings such as:
- `"Deleted parent ${parent.email}"`
- `"Updated parent ${saved.email}"`
- `"Added ${parent.email} as $role"`
- `"Revoked admin access for ${parent.email}"`

These are stored in the database and may be exported or viewed in admin tools. COMPLIANCE.md data inventory states *"Audit logs (email masked)"*.

**Risk:** Unauthorised access to audit tables or exports exposes parent email addresses. Compliance and policy require masking.

**Remediation:**  
- When writing `details`, mask email with `PiiMask.maskEmail(...)` for any parent email included in the string.  
- Alternatively, store only resource type and ID in `details` and resolve email only in controlled admin UI with access control and audit.  
- Document the chosen approach in the Audit Logging Policy.

---

### 2.3 MEDIUM — RefreshTokenRequest has no size limit

**Rule / standard:** Input validation; reject oversized payloads (project security rules, OWASP).

**Evidence:**  
`backend/.../api/auth/dto/RefreshTokenRequest.kt` has `@NotBlank` on `refreshToken` but no `@Size(max=...)`. A client could send an arbitrarily large body.

**Risk:** DoS via large request bodies or excessive memory; potential for parser issues.

**Remediation:**  
- Add `@Size(max = 2048)` (or another reasonable limit consistent with JWT size) to `refreshToken`.  
- Ensure any other auth DTOs (e.g. OTP, passwordless) have bounded string lengths.

---

### 2.4 MEDIUM — Admin API excluded from general rate limiting

**Rule / standard:** Rate limiting on sensitive endpoints (project rules, OWASP).

**Evidence:**  
`RateLimitingFilter` skips rate limiting for paths containing `/api/v1/admin` so the admin dashboard can make many parallel requests.

**Risk:** If an admin JWT is compromised, an attacker can issue unbounded requests to admin endpoints (e.g. data export, user listing) without being throttled. No brute-force or enumeration protection on admin paths.

**Remediation:**  
- Apply a separate, higher limit for admin paths (e.g. per-admin principal) rather than excluding them entirely.  
- Or use a dedicated admin rate-limit bucket (e.g. 200/min per admin) and keep general limit for unauthenticated/auth endpoints.  
- Ensure admin tokens have short expiry and refresh policy.

---

### 2.5 MEDIUM — Swagger UI and Actuator publicly accessible

**Rule / standard:** Minimise attack surface; no unnecessary info disclosure in production (OWASP, SOC 2).

**Evidence:**  
`SecurityConfig` permits unauthenticated access to:
- `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`
- `/actuator/health/**`, `/actuator/info`, `/actuator/prometheus`

**Risk:** In production, API structure and metrics are visible to anyone. Prometheus can expose internal metrics; Swagger reveals endpoints and shapes. Increases reconnaissance and potential abuse.

**Remediation:**  
- In production profile, require authentication (or IP allowlist) for `/v3/api-docs/**`, `/swagger-ui/**`, and for actuator endpoints other than a minimal health check.  
- Or expose Swagger and detailed actuator only on an internal port/profile.  
- Keep a single public health endpoint (e.g. `/actuator/health`) for load balancers.

---

### 2.6 LOW — Hardcoded dev admin credentials in source

**Rule / standard:** No credentials in source (project rules, SOC 2).

**Evidence:**  
`DevSeedController` defines `ADMIN_EMAIL = "admin@techadivas.com"` and `ADMIN_PASSWORD = "Admin123!"`. Same credentials appear in `scripts/seed-admin.sql`, `.env.example`, `docker-compose.yml`, and admin login page copy.

**Risk:** Low **provided** dev profile and `SEED_ADMIN_ENABLED` are never enabled in production. If misconfiguration enables dev seed in prod, default admin is predictable.

**Remediation:**  
- Keep controller and seed script under `@Profile("dev")` and enforce `SEED_ADMIN_ENABLED=false` in production (config/startup check).  
- Consider reading dev admin password from env (e.g. `DEV_SEED_ADMIN_PASSWORD`) when seeding, so it is not in repo.  
- Document in runbooks that prod must not use `dev` profile or seed.

---

### 2.7 LOW — Rate limiter trusts X-Forwarded-For

**Rule / standard:** Use of client-controlled headers for security decisions (OWASP).

**Evidence:**  
`RateLimitingFilter.clientKey()` uses `X-Forwarded-For` when present, then falls back to `remoteAddr`.

**Risk:** If the app is not behind a trusted proxy that overwrites or validates `X-Forwarded-For`, a client can spoof the header and evade or abuse rate limits (e.g. spread requests across many “IPs”).

**Remediation:**  
- In production, run behind a trusted reverse proxy (e.g. ALB, nginx) that sets `X-Forwarded-For` from the real client IP.  
- Optionally restrict `X-Forwarded-For` to known proxy IPs and use the rightmost trusted value.  
- Document proxy and rate-limit behaviour in deployment docs.

---

### 2.8 LOW — Webhook payload encryption optional

**Rule / standard:** COMPLIANCE.md §2.3 (PCI): webhook payloads at rest should be encrypted when Stripe is enabled in production.

**Evidence:**  
`WebhookEventRepositoryAdapter` uses `WebhookPayloadEncryptor.encryptIfConfigured(...)`; encryption is applied only when `appProperties.subscription.webhookPayloadEncryptionKey` is set.

**Risk:** If the key is not set in production, Stripe (and Zoho) webhook payloads are stored unencrypted. They may contain customer/payment identifiers.

**Remediation:**  
- In production, when Stripe (or Zoho) webhooks are enabled, require `SUBSCRIPTION_WEBHOOK_ENCRYPTION_KEY` (or equivalent) and fail startup if missing.  
- Document in deployment checklist and COMPLIANCE.md.

---

### 2.9 INFORMATIONAL — Admin JWT in localStorage

**Evidence:**  
Admin app stores `admin_access_token` and `admin_refresh_token` in `localStorage` (`admin/src/lib/api.ts`).

**Risk:** XSS in the admin app could exfiltrate tokens. Same-origin policy limits exposure but does not eliminate it.

**Remediation:**  
- Prefer httpOnly, Secure, SameSite cookies for admin tokens if the backend can set cookies and admin is same-site.  
- If localStorage is retained: strict CSP, avoid embedding user-controlled data in DOM/scripts, and consider short-lived access tokens with refresh in memory where possible.  
- Document as accepted risk if cookies are not feasible and controls are in place.

---

### 2.10 INFORMATIONAL — TwilioSmsSender PII fix verified

**Evidence:**  
Previous audit noted logging of full phone and OTP. Code now uses `PiiMask.maskPhone(toPhone)` and does not log the OTP code. **No action** other than to keep this under review in future changes.

---

### 2.11 INFORMATIONAL — LoginRequest size limits in place

**Evidence:**  
`LoginRequest` has `@Size(max=255)` for email and `@Size(max=500)` for password. **No further action** for this DTO.

---

### 2.12 INFORMATIONAL — Compliance and operations

The following are documented in COMPLIANCE.md or existing docs but are operational rather than code findings:

- **PCI scope:** Document that card data is handled only by Stripe (SAQ A or equivalent) and that Tamixa does not store/process cardholder data.  
- **Penetration testing:** Annual (or post-major-change) pen test with findings tracked and remediated.  
- **Security monitoring:** Sentry/Datadog (or equivalent) for auth failures, 5xx spikes, webhook signature failures.  
- **Incident response:** Keep `docs/INCIDENT_RESPONSE.md` and contacts up to date; test periodically.

---

## 3. Positive Controls Observed

- **Secrets:** No hardcoded API keys, JWT secret, or DB passwords; `.env` gitignored; `.env.example` template only.  
- **AuthZ:** Parent and admin endpoints use `@PreAuthorize`; resource ownership enforced (e.g. story parentId, subscription, voice profile).  
- **Input:** Auth DTOs use `@Valid`; RegisterRequest/LoginRequest have size/format constraints; story theme/childName sanitised and length-limited in `StorySafetyMiddleware`; no raw SQL concatenation.  
- **CORS:** `CorsOriginValidator` fails prod startup if `CORS_ALLOWED_ORIGINS` is unset or `*`.  
- **Rate limiting:** General and story-generation rate limits with tiering; `X-RateLimit-*` and `Retry-After` used.  
- **Webhooks:** Stripe (and Zoho) signature verification; idempotency; webhook payload encryption when key is set.  
- **Content safety:** Moderation and safety checks; prompt injection and blocklist in `StorySafetyMiddleware`.  
- **PII in logs:** AuthController and OTP use `PiiMask.maskEmail` / `maskPhone`; JWT filter logs masked email.  
- **Android tokens:** Stored in EncryptedSharedPreferences (AES256-GCM).  
- **Errors:** No stack traces or internal paths in client-facing error responses.

---

## 4. Compliance Snapshot

| Area | Status | Notes |
|------|--------|--------|
| **DPDP (India)** | Aligned | Consent, purpose limitation, rights, children’s data; document retention and breach process. |
| **COPPA** | Aligned | Parent-only registration; consent; minimal collection; no child accounts. |
| **GDPR** | Aligned | Lawful basis, consent, rights, security; DPA and breach procedure. |
| **PCI** | Aligned | No card storage; Stripe; webhook verification; recommend encrypted webhook storage in prod. |
| **SOC 2 readiness** | Partial | Access control, encryption, audit; address log PII and operational monitoring. |

---

## 5. Remediation Priorities

1. **Immediate (High):**  
   - Migrate iOS token storage from NSUserDefaults to Keychain.  
   - Mask or remove parent email from admin audit `details` and update policy.

2. **Short term (Medium):**  
   - Add `@Size(max=2048)` to `RefreshTokenRequest.refreshToken`.  
   - Introduce admin-specific rate limiting (no full bypass).  
   - Restrict Swagger and actuator in production (auth or internal only).

3. **Next sprint (Low):**  
   - Enforce webhook encryption key in prod when webhooks enabled.  
   - Document X-Forwarded-For and proxy trust; consider validation.  
   - Move dev seed password to env and document prod restrictions.

4. **Ongoing (Informational):**  
   - Consider httpOnly cookies for admin; maintain CSP and XSS hygiene.  
   - Complete PCI scope statement; schedule pen test; finalise monitoring/alerting and incident runbooks.

---

## 6. Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | March 2026 | Security & Compliance Audit | Initial auditor-style report |

---

*This report is for internal use to improve security and compliance posture. Findings should be tracked in your issue tracker and re-verified after remediation.*
