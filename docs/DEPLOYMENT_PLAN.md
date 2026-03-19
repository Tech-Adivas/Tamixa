# Tamixa Deployment Plan

**Role:** Senior Delivery Lead  
**Last updated:** March 2025  
**Status:** Pre-production readiness

---

## Executive Summary

This plan covers deployment of the **Tamixa** application—a multi-component system with:

| Component | Stack | Artifact | Hosting |
|-----------|-------|----------|---------|
| **Backend** | Kotlin, Spring Boot 21 | Docker image | Container / PaaS |
| **Web** | React, Vite | Static `dist/` | Vercel, S3+CloudFront, etc. |
| **Admin** | Next.js | Node app / static | Vercel, Render, etc. |
| **Mobile** | KMP, Compose | APK/AAB | Play Store |

**Critical gap:** The web app currently hardcodes `http://localhost:8080` for the API. Production builds must use `VITE_API_URL` (see Phase 1).

---

## Phase 0: Pre-flight Checklist

Before any deployment work:

- [ ] **CI green** – All jobs pass on `main` / `develop` (`.github/workflows/ci.yml`)
- [ ] **Clean tree** – No uncommitted changes; deploy from tagged commit or CI-built artifacts
- [ ] **Branch strategy** – `main` = production; `develop` = integration; PR + CI required for merge
- [ ] **Secrets** – No secrets in repo; use env vars or secrets manager
- [ ] **Dev endpoints off** – `SEED_ADMIN_ENABLED=false`, `DEV_OTP_CODE` unset in prod

---

## Phase 1: Pre-deployment Fixes (Blocking)

### 1.1 Web production API URL (Required)

**Problem:** `config/api.config.ts` exports `DEFAULT_API_BASE_URL = "http://localhost:8080"`. The web app uses this for all API calls. Production builds will point to localhost and fail.

**Fix:**

1. In `config/api.config.ts`, add support for `import.meta.env.VITE_API_URL`:
   ```ts
   export const DEFAULT_API_BASE_URL =
     (typeof import.meta !== "undefined" && import.meta.env?.VITE_API_URL) ||
     "http://localhost:8080";
   ```
2. In `scripts/build-production.sh`, for the web step, pass:
   ```bash
   VITE_API_URL="$API_URL" npm run build
   ```

**Verification:** Build web with `VITE_API_URL=https://api.tamixa.com npm run build` and confirm network requests go to that URL.

### 1.2 Build script alignment

- **Admin:** Already uses `NEXT_PUBLIC_API_URL="$API_URL"` ✓
- **Web:** Add `VITE_API_URL="$API_URL"` (see 1.1)
- **Mobile:** Uses `-PTAMIXA_API_BASE_URL="$API_URL"` ✓

---

## Phase 2: Backend Production Environment

### Required variables (startup fails without these)

| Variable | Purpose |
|----------|---------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `JWT_SECRET` | ≥256 bits; HS256 secret |
| `CORS_ALLOWED_ORIGINS` | Comma-separated, e.g. `https://app.tamixa.com,https://admin.tamixa.com` (no `*`) |
| `DATABASE_URL` | Full JDBC URL |
| `DATABASE_USERNAME` | DB user |
| `DATABASE_PASSWORD` | DB password |
| `OPENAI_API_KEY` | Story generation, translation |
| `VOICE_ENCRYPTION_KEY` | 32-byte Base64 AES for voice embeddings |
| `MAGIC_LINK_BASE_URL` | e.g. `https://app.tamixa.com` |

### Optional (feature-dependent)

| Variable | Default | Notes |
|----------|---------|-------|
| `TRANSLATION_PROVIDER` | `simulated` | Set `openai` for prod |
| `REDIS_HOST`, `REDIS_PORT` | localhost:6379 | For caches / rate limiting |
| `S3_BUCKET`, `S3_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` | — | Audio storage |
| `KAFKA_BOOTSTRAP_SERVERS` | — | Async pipeline; can defer to sync |
| `STRIPE_*`, `ZOHO_*` | — | Payments |
| `SENDGRID_API_KEY` | — | Magic link emails |
| `SUBSCRIPTION_WEBHOOK_ENCRYPTION_KEY` | — | 32-byte Base64; required when Stripe webhooks enabled |

### Security (prod only)

- `SEED_ADMIN_ENABLED=false`
- `DEV_OTP_CODE` unset
- Stripe/Zoho webhook secrets from secrets manager
- Run behind trusted reverse proxy for `X-Forwarded-For` (rate limiting)

---

## Phase 3: Infrastructure & Hosting

### 3.1 Hosting options

| Option | Backend | Web | Admin | DB / Redis / Kafka |
|--------|---------|-----|-------|--------------------|
| **Railway** | Docker | Static | Service | Managed Postgres + Redis |
| **Render** | Docker | Static | Web service | Managed Postgres + Redis |
| **Fly.io** | Fly app | Static | Fly app | Fly Postgres; external Redis/Kafka |
| **AWS** | ECS / Cloud Run | S3+CloudFront | Same or separate | RDS, ElastiCache, MSK |
| **GCP** | Cloud Run | Static / Firebase | Same | Cloud SQL, Memorystore, Pub/Sub |

### 3.2 Provision

- **PostgreSQL 16** – Run Flyway on startup (or dedicated migration job with `FLYWAY_ENABLED=true`)
- **Redis 7** – If using rate limiting / caches
- **S3 (or equivalent)** – Bucket + IAM for audio/assets
- **Kafka** – Optional for MVP; sync story generation works without it

### 3.3 DNS & TLS

- `api.tamixa.com` → Backend
- `app.tamixa.com` → Web
- `admin.tamixa.com` → Admin
- TLS at load balancer or reverse proxy

---

## Phase 4: Deploy Backend

1. **Build image**
   ```bash
   docker build -t tamixa-backend:prod .
   ```

2. **Run with prod env**
   - Set all required env vars (see Phase 2)
   - Use `DATABASE_*` (not `POSTGRES_*`) for prod
   - Expose port 8080 behind HTTPS

3. **Health**
   - Readiness: `/actuator/health/readiness`
   - Liveness: `/actuator/health`

4. **Verify**
   ```bash
   curl https://api.tamixa.com/actuator/health
   ```

---

## Phase 5: Deploy Web

1. **Build**
   ```bash
   API_URL=https://api.tamixa.com ./scripts/build-production.sh --web-only
   ```
   Or: `cd web && VITE_API_URL=https://api.tamixa.com npm run build`

2. **Host**
   - Deploy `web/dist/` to Vercel, Netlify, Cloudflare Pages, S3+CloudFront, or nginx

3. **Routing**
   - SPA fallback: all routes serve `index.html`

4. **Verify**
   - Open `https://app.tamixa.com`, login/register, confirm API calls hit `https://api.tamixa.com`

---

## Phase 6: Deploy Admin

1. **Build**
   ```bash
   API_URL=https://api.tamixa.com ./scripts/build-production.sh --admin-only
   ```

2. **Host**
   - Vercel, Render, Fly, or Docker
   - Restrict access (VPN, IP allowlist) if possible

3. **Verify**
   - Log in with ADMIN user; confirm dashboard and API calls work

---

## Phase 7: Mobile Release (Android)

1. **Build**
   ```bash
   ./gradlew :composeApp:assembleRelease \
     -PTAMIXA_API_BASE_URL=https://api.tamixa.com \
     -PTAMIXA_WEB_APP_URL=https://app.tamixa.com
   ```

2. **Signing**
   - Configure keystore in `mobile/composeApp/build.gradle.kts`
   - Produce signed AAB for Play Console

3. **Distribute**
   - Internal track first; production when approved

4. **Verify**
   - Register → Add child → Generate story → Play → Subscription (Manage opens web)

---

## Phase 8: Post-deploy Verification

### Smoke tests

| Component | Checks |
|-----------|--------|
| Backend | Health, readiness; login/register; one protected endpoint |
| Web | Register → Login → Dashboard → Add child → Generate story → Play → Subscription → Logout |
| Admin | Login (ADMIN) → Dashboard → Parents / Stories / Subscriptions |
| Mobile | Same flow as Phase 7; confirm API and web URLs are prod |

### Monitoring

- Uptime checks on `https://api.tamixa.com/actuator/health`
- Optional: Sentry for errors
- Rollback: Keep previous Docker image; document redeploy steps

---

## Phase 9: CI/CD Deploy (Optional)

Add deploy job to `.github/workflows/ci.yml`:

- Trigger: push to `main` or tag
- Build backend Docker image → push to registry
- Build web/admin with `API_URL` → deploy to host
- Use GitHub secrets for `API_URL`, `JWT_SECRET`, `DATABASE_*`, etc.
- Keep steps idempotent and reversible

---

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Web points to wrong API | Implement Phase 1.1 before go-live |
| Kafka not ready | Use sync story generation; add Kafka later |
| S3 not configured | Audio streaming fails; set S3 vars before launch |
| Play Store delay | Use internal testing track first |
| Dev seed in prod | `SEED_ADMIN_ENABLED=false`; prod profile only |

---

## File Reference

| Purpose | File |
|---------|------|
| Backend prod config | `backend/src/main/resources/application-prod.yml` |
| Backend Docker | `Dockerfile` |
| Env template | `.env.example` |
| Build script | `scripts/build-production.sh` |
| Admin deploy | `admin/DEPLOYMENT.md` |
| Full checklist | `docs/DEPLOYMENT_CHECKLIST.md` |

---

## Quick Command Reference

```bash
# Full production build
API_URL=https://api.tamixa.com ./scripts/build-production.sh

# Backend only
docker build -t tamixa-backend:prod .

# Web only (after Phase 1.1)
cd web && VITE_API_URL=https://api.tamixa.com npm run build

# Admin only
cd admin && NEXT_PUBLIC_API_URL=https://api.tamixa.com npm run build

# Mobile release
./gradlew :composeApp:assembleRelease -PTAMIXA_API_BASE_URL=https://api.tamixa.com -PTAMIXA_WEB_APP_URL=https://app.tamixa.com
```
