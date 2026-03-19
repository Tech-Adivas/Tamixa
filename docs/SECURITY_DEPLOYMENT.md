# Security & deployment

Short reference for security-sensitive deployment choices. See [SECURITY_COMPLIANCE_AUDIT_REPORT.md](SECURITY_COMPLIANCE_AUDIT_REPORT.md) and [COMPLIANCE.md](COMPLIANCE.md) for full audit and compliance context.

---

## Reverse proxy and X-Forwarded-For

The backend **rate limiter** identifies the client by IP. It uses:

1. **`X-Forwarded-For`** header (first value in the list), if present  
2. Otherwise **`request.remoteAddr`**

**Implications:**

- If the backend is **not** behind a proxy, `remoteAddr` is the real client IP and behaviour is correct.
- If the backend **is** behind a reverse proxy (recommended in production), the proxy should set `X-Forwarded-For` to the **client** IP. The backend then uses that for rate limiting.
- If clients can reach the backend directly and send their own `X-Forwarded-For`, they could spoof the header and evade or abuse rate limits.

**Recommendations:**

- In production, place the backend behind a **trusted** reverse proxy (e.g. AWS ALB, nginx, Cloud Run) so that only the proxy talks to the app. The proxy should set `X-Forwarded-For` from the real client IP.
- Optionally restrict acceptance of `X-Forwarded-For` to requests from known proxy IPs and use the rightmost trusted value per your proxy’s behaviour.
- Document in runbooks which proxy is in front of the backend and that rate limiting relies on it.

---

## Dev seed and production

- **`POST /api/v1/dev/seed-admin`** creates or resets an admin user (e.g. `admin@techadivas.com` with a default password). It is only available when:
  - The **`dev`** profile is active, and  
  - **`SEED_ADMIN_ENABLED=true`** (or equivalent config).
- **Production must not use the dev profile** for the main app. Use `SPRING_PROFILES_ACTIVE=prod`. With `prod` profile, the dev seed controller is not loaded.
- **Never** set `SEED_ADMIN_ENABLED=true` in production. Keep dev credentials (email/password) out of production config and runbooks; use them only in local or CI dev environments.
- In deployment checklists, confirm that production does not have `dev` profile active and does not enable seed admin.

---

## Webhook encryption (Stripe)

When **Stripe webhooks** are enabled in **production** (profile `prod`), the application **requires** `SUBSCRIPTION_WEBHOOK_ENCRYPTION_KEY` (32-byte Base64 AES key) so that webhook payloads stored at rest are encrypted. Startup fails if the key is missing. Set this in your production environment when Stripe is enabled.

---

## CORS

In production, set **`CORS_ALLOWED_ORIGINS`** to a comma-separated list of allowed origins (e.g. `https://app.tamixa.com,https://admin.tamixa.com`). Do not use `*` when credentials are allowed. Startup fails in prod if CORS is unset or `*`.
