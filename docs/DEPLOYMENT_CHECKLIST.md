# Tamixa Deployment Checklist

Pre-release checklist for Day 3 and production deployment.

---

## 0. Production Build (All Artifacts)

```bash
# Build everything (Docker, web, admin, mobile)
./scripts/build-production.sh

# Or build selectively:
./scripts/build-production.sh --backend-only
./scripts/build-production.sh --web-only --admin-only

# Custom API URL:
API_URL=https://api.yourdomain.com ./scripts/build-production.sh
```

---

## 1. Mobile Release Build

### Build commands
```bash
cd mobile
./gradlew :composeApp:assembleRelease
```

APK output: `mobile/composeApp/build/outputs/apk/release/composeApp-release-unsigned.apk`

### Signing (production)
Configure ` signingConfigs` in `build.gradle.kts` with your keystore, then use `assembleRelease` to produce a signed APK.

### ProGuard / R8
- Rules in `mobile/composeApp/proguard-rules.pro` cover: Ktor/slf4j, Kotlin serialization, domain/network models
- Release build succeeds with current rules
- Kotlin metadata warnings from R8 are non-fatal (Kotlin 2.x vs R8 version mismatch)

### Release overrides (Gradle properties or env)
- `TAMIXA_API_BASE_URL` – Production API URL (default: `https://api.tamixa.com`)
- `TAMIXA_WEB_APP_URL` – Web app URL for subscription (default: `https://app.tamixa.com`)

---

## 2. Backend Environment Variables

### Required (production)
| Variable | Description | Notes |
|----------|-------------|-------|
| `JWT_SECRET` | HS256 secret (min 256 bits) | **Required in prod**; startup fails if default dev secret is used |
| `DATABASE_URL` | PostgreSQL JDBC URL | Or `SPRING_DATASOURCE_URL` |
| `DATABASE_USERNAME` | DB username | Or `SPRING_DATASOURCE_USERNAME` |
| `DATABASE_PASSWORD` | DB password | Or `SPRING_DATASOURCE_PASSWORD` |
| `AI_LLM_PROVIDER` | `openai` (default) or `gemini` | Which provider backs story generation, moderation, pipeline rewrite |
| `OPENAI_API_KEY` | OpenAI API key | Required when `AI_LLM_PROVIDER=openai` (and when `AI_COVER_IMAGE_PROVIDER=openai` for AI covers) |
| `GEMINI_API_KEY` | Google AI (Gemini) API key | Required when `AI_LLM_PROVIDER=gemini`, `TRANSLATION_PROVIDER=gemini`, `AI_COVER_IMAGE_PROVIDER=gemini`, and/or `COVER_ANIMATION_VEO_ENABLED=true` |
| `AI_COVER_IMAGE_PROVIDER` | `openai` or `gemini` (default `openai`) | Still cover: DALL·E 3 vs Gemini native image |
| `VOICE_ENCRYPTION_KEY` | Base64 32-byte AES key | Voice embedding encryption |

### Optional (feature flags / scaling)
| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `dev` | Use `prod` for production |
| `REDIS_HOST` | `localhost` | Redis for caches |
| `REDIS_PORT` | `6379` | |
| `KAFKA_BOOTSTRAP_SERVERS` | — | **Not used by the Spring app today** (narration is in-process). Compose may still start Kafka for future/async work; safe to omit. |
| `MAGIC_LINK_BASE_URL` | `http://localhost:3000` | Web app URL for magic links |
| `SENDGRID_API_KEY` | — | Real magic link emails |
| `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER` | — | Real OTP SMS |
| `DEV_OTP_CODE` | — | Dev-only bypass (e.g. `123456`) for OTP |
| `CORS_ALLOWED_ORIGINS` | — | **Required in prod**; startup fails if `*` or unset. Use comma-separated origins (e.g. `https://app.tamixa.com,https://admin.tamixa.com`) |

### Subscription / payments
| Variable | Default | Description |
|----------|---------|-------------|
| `STRIPE_ENABLED` | `false` | Enable Stripe |
| `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET` | — | Stripe config |
| `SUBSCRIPTION_WEBHOOK_ENCRYPTION_KEY` | — | **Required in prod when Stripe webhooks enabled.** 32-byte Base64 AES key to encrypt webhook payloads at rest (PCI/compliance). |
| `ZOHO_ENABLED` | `false` | Enable Zoho Payments |
| `ZOHO_ACCOUNT_ID`, `ZOHO_WEBHOOK_SIGNING_KEY` | — | Zoho Payments config (webhook: POST /api/v1/webhooks/zoho) |

### Rate limiting
| Variable | Default | Description |
|----------|---------|-------------|
| `RATE_LIMIT_REQUESTS_PER_MINUTE` | `100` | General API limit per client IP. |
| `RATE_LIMIT_ADMIN_REQUESTS_PER_MINUTE` | `200` | Admin API limit per client IP (no full bypass). |

---

## 2.1 Security & deployment (proxy, dev-seed)

### Reverse proxy and X-Forwarded-For

Rate limiting uses the client IP derived from `X-Forwarded-For` (if present) or `request.remoteAddr`. **In production:**

- Run the backend behind a **trusted** reverse proxy (e.g. ALB, nginx, Cloud Run) that sets `X-Forwarded-For` from the real client IP.
- Do not allow untrusted clients to reach the backend directly; otherwise a client could spoof `X-Forwarded-For` and evade or abuse rate limits.
- Document which proxy IPs are trusted if you restrict or validate the header.

See [SECURITY_DEPLOYMENT.md](SECURITY_DEPLOYMENT.md) for details.

### Dev seed and production

- **`SEED_ADMIN_ENABLED`** and **`POST /api/v1/dev/seed-admin`** are for **development only**. They create or reset an admin user (e.g. `admin@techadivas.com` / default password).
- **Never** set `SEED_ADMIN_ENABLED=true` in production. In production use `SPRING_PROFILES_ACTIVE=prod`; the dev seed controller is only loaded when the `dev` profile is active.
- Ensure production environment does not activate the `dev` profile and does not expose `/api/v1/dev/*` to the internet if the profile were ever enabled.

---

## 2.2 Database migrations (Flyway)

Scripts live in `backend/src/main/resources/db/migration/`. Spring Boot runs Flyway on startup when **`FLYWAY_ENABLED`** is not set to `false` (default is **enabled**; see `application.yml`).

**Before deploying a backend build that expects new schema:**

1. **App runs Flyway on boot (typical dev / many prod setups)** — Deploy the new image or JAR; the first healthy start applies any pending migrations automatically. No separate step unless Flyway is disabled.
2. **Flyway disabled on application nodes (`FLYWAY_ENABLED=false`)** — Run migrations in a **dedicated release/migrate job** (or one-off task) with the same artifact and DB URL, **`FLYWAY_ENABLED=true`**, **before** rolling out new app replicas that depend on the new columns.

**Example — V70 (`V70__regenerate_prompt_gate.sql`):** adds `regenerate_prompt_locked`, `regenerate_prompt_lock_approved`, and `regenerate_prompt_unlock_requested_at` on `library_stories` for admin “Regenerate with prompt” gating after machine translation. If this migration has not run, Hibernate schema validation or runtime SQL can fail against an older database.

Verify applied versions: check table `flyway_schema_history` in PostgreSQL (or logs on first startup after deploy).

---

## 3. Admin Auth Verification

### Requirements
- Backend: `POST /api/v1/auth/login`, `GET /api/v1/auth/me`, `POST /api/v1/auth/refresh`
- At least one user with `ADMIN` role in the database

### Verification steps
1. Start backend: `./gradlew :backend:bootRun`
   - For SendGrid (magic links), load `.env` first: `set -a && source .env 2>/dev/null; set +a`
2. Start admin: `cd admin && npm run dev`
3. Open `http://localhost:3000` (or admin port)
4. Log in with an ADMIN user
5. Confirm dashboard loads and API calls succeed
6. Set `NEXT_PUBLIC_API_URL` in `admin/.env.local` to your backend URL

### Mock mode
- Set `NEXT_PUBLIC_USE_MOCK_API=true` for KPI/monitoring mock data when backend is unavailable
- Login still uses real backend for JWT

---

## 4. Smoke Test (End-to-End)

### Mobile flow
1. **Register** – Create account (email/password or passwordless code)
2. **Language selection** – Select Tamil or English
3. **Add child** – Create child profile with name, DOB, interests
4. **Generate story** – Select child, theme; wait for generation
5. **Play audio** – Open story, tap play; verify streaming works
6. **Voice upload** – Pick audio file; verify upload and profile appear
7. **Subscription** – Open subscription; view plan and usage; tap Manage → opens web subscription page
8. **Settings** – Toggle dark mode; verify persistence on app restart
9. **Logout** – Sign out; verify return to login

### Web flow
1. Login at `/login` (passwordless or password)
2. Dashboard at `/dashboard`
3. Children at `/children` – add child
4. Stories at `/stories` – generate story
5. Subscription at `/subscription` – view plan, cancel at period end if applicable
6. Logout

### Backend health
```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/health/readiness
```

### Kubernetes / container probes (Phase 1)

Point your workload at Spring Boot Actuator (see `management.endpoint.health` in `application.yml`):

| Probe | Path | Purpose |
|-------|------|---------|
| **Liveness** | `GET /actuator/health/liveness` | JVM up; restart pod if failing. |
| **Readiness** | `GET /actuator/health/readiness` | DB + Redis + diskSpace; remove from service when not ready. |

Example (adjust port and scheme):

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 10
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 5
```

**Metrics:** scrape `GET /actuator/prometheus` from inside the cluster or a dedicated metrics collector (actuator web exposure is configured in `application.yml`; protect with network policy or auth in production).

### Automated E2E (Playwright)
With web and admin dev servers running:
```bash
cd e2e && npm ci && npx playwright install chromium && npm run test:smoke
```
See `e2e/README.md` for full flows and env vars.

---

## 5. Backend Tests

```bash
# Backend-only (skip mobile/Android SDK)
./gradlew :backend:test -Ptamixa.backendOnly=true
```

**Requirements:** Docker for Testcontainers (PostgreSQL). Tests use `application-test` profile; Redis and Kafka are excluded; readiness health group is overridden to exclude Redis.

**Note:** Some tests may be flaky due to Testcontainers startup timing. Compilation issues in AuthApiIntegrationTest and AuthConsentIntegrationTest have been fixed (use `exchange` with `ParameterizedTypeReference` instead of `postForEntity` with `Map::class.java`).

---

## 6. Kafka (optional / future only)

The **backend does not publish or consume Kafka** today; story narration uses **in-process** workers (`InlineStoryEventPublisher`). If you introduce a real async pipeline later, create topics as needed, for example:

```bash
kafka-topics --create --topic story-created --bootstrap-server <broker>
kafka-topics --create --topic story-created.DLT --bootstrap-server <broker>
kafka-topics --create --topic curated-story-created --bootstrap-server <broker>
```

See [ARCHITECTURE.md](ARCHITECTURE.md).

---

## Quick reference

| Component | Run command | Port |
|-----------|-------------|------|
| Backend | `./gradlew :backend:bootRun` | 8080 |
| Web | `cd web && npm run dev` | 3000 |
| Admin | `cd admin && npm run dev` | 3001 |
| Mobile | `cd mobile && ./gradlew :composeApp:installDebug` | — |
