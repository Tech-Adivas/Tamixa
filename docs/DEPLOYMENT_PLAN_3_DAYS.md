# Tamixa Kids — 3-Day Production Deployment Plan

**Goal:** Deploy to production by **Sunday, March 1, 2025**

---

## Current State Summary

| Component | Status | Notes |
|-----------|--------|-------|
| **Backend** | Ready | Spring Boot, PostgreSQL, Redis, Kafka; production config exists |
| **Web** | Ready | React + Vite; needs prod API URL at build time |
| **Admin** | Ready | Next.js standalone; needs prod API URL |
| **Mobile** | Ready | KMP Android; needs prod BASE_URL in release build |
| **CI** | Ready | GitHub Actions: backend, web, admin build + test |
| **Dockerfile** | ⚠️ Broken | Root Dockerfile copies `src` but backend is in `backend/` |

---

## Day 1 — Thursday (Prep + Fixes + Staging)

### 1.1 Fix Root Dockerfile

The root `Dockerfile` is built for a flat layout. Fix it for the monorepo:

- **Context:** `context: .` (root)  
- **Copy:** `backend/` and root Gradle files  
- **Build:** `./gradlew :backend:bootJar -Ptamixa.backendOnly=true -x test`  
- **Output:** `backend/build/libs/*.jar`

### 1.2 Create Production Build Variants (Mobile)

- Add `buildTypes.release` with production API URL (BuildConfig)
- Option: use Gradle `buildConfigField` with env/property, e.g. `BASE_URL` = production API URL
- Document in `mobile/README.md` how to build release with `BASE_URL`

### 1.3 Environment Config

- **Backend prod env vars** (see `PRODUCTION_UPGRADE.md`):
  - `JWT_SECRET` (256+ bits)
  - `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
  - `OPENAI_API_KEY`, `VOICE_ENCRYPTION_KEY`
  - `TRANSLATION_PROVIDER=openai` (real translation; default is simulated)
  - S3: `S3_BUCKET`, `S3_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`
  - Optional: `CORS_ALLOWED_ORIGINS`, `CDN_STREAM_ENABLED`
- **Web:** `VITE_API_BASE_URL` or equivalent for prod build (adjust `config/api.config.ts` if needed)
- **Admin:** `NEXT_PUBLIC_API_URL` for build
- **config/api.config.json:** Update baseUrl for prod (or use env override everywhere)

### 1.4 Staging Smoke Test (Local / Docker)

- Start Postgres, Redis, Kafka, backend via `docker-compose`
- Run backend with `SPRING_PROFILES_ACTIVE=prod` and a test DB
- Verify:
  - `/actuator/health`
  - Register / login flow
  - Story list, generation (if OpenAI key set)
  - Web + Admin pointing to backend

---

## Day 2 — Friday (Infrastructure + Deploy Backend + Web + Admin)

### 2.1 Choose Hosting

Pick one and stick to it:

| Option | Backend | Web | Admin | DB / Redis / Kafka | Notes |
|--------|---------|-----|-------|--------------------|-------|
| Railway | ✅ | ✅ | ✅ | Managed Postgres/Redis; Kafka → Confluent Cloud | Simple |
| Render | ✅ | ✅ | ✅ | Managed Postgres/Redis; Kafka → external | Good free tier |
| Fly.io | ✅ | ✅ | ✅ | Fly Postgres; Redis/Kafka on Fly or external | Global edge |
| AWS/GCP | ECS/GKE | S3+CF / Cloud Run | Same | RDS/Cloud SQL, ElastiCache, MSK/Cloud Pub-Sub | More control |

### 2.2 Provision Infrastructure

- PostgreSQL (prod)
- Redis (prod)
- Kafka (managed) or disable initially if not critical for MVP
- Domain: e.g. `api.tamixa.com`, `app.tamixa.com`, `admin.tamixa.com`

### 2.3 Deploy Backend

- Build image: fix `Dockerfile` → `docker build -t tamixa-backend:prod .`
- Set prod env vars on host
- Expose only needed port (8080) behind TLS (load balancer or reverse proxy)
- Run health checks: `/actuator/health`

### 2.4 Deploy Web

- Build: `cd web && npm run build` with prod API URL
- Deploy to static hosting (Vercel, Netlify, Cloudflare Pages, S3+CloudFront) or serve via nginx
- Ensure `/api` proxy or client points to `https://api.tamixa.com`

### 2.5 Deploy Admin

- Build: `cd admin && npm run build` with `NEXT_PUBLIC_API_URL`
- Deploy to Vercel / Render / Docker
- Restrict to admin users only (backend role check)

### 2.6 Update Magic Link Base URL

- Set `app.magic-link.base-url` (or `MAGIC_LINK_BASE_URL`) to `https://app.tamixa.com` (web app URL)

---

## Day 3 — Saturday (Mobile + E2E + Final Checklist)

### 3.1 Mobile Release Build (Android)

- Set `BASE_URL` in `composeApp` / `androidApp` build.gradle.kts for release
- Build: `./gradlew :composeApp:assembleRelease`
- Sign APK/AAB (keystore)
- Option: Upload to Play Console (internal track) for testing

### 3.2 End-to-End Tests

- Register → Login → Create child → Generate story → Play audio
- Admin: login → curated stories → parents
- Web: magic link flow

### 3.3 Production Checklist

- [ ] JWT_SECRET changed from default
- [ ] TRANSLATION_PROVIDER=openai (real translations; default is simulated)
- [ ] S3 credentials set (S3_BUCKET, S3_REGION, AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
- [ ] OPENAI_API_KEY set for story generation and translation
- [ ] HTTPS everywhere
- [ ] CORS restricted to prod origins
- [ ] DB credentials not in code
- [ ] Rate limiting enabled (backend)
- [ ] Screenshot prevention on Login/Register (mobile)

### 3.4 Monitoring

- Actuator health + Prometheus (if applicable)
- Error tracking (optional: Sentry)
- Uptime check (e.g. UptimeRobot)

---

## Sunday — Go-Live

### Morning

- Final smoke test on prod
- Mobile: Submit to Play Store (if ready) or distribute via internal track
- DNS cutover if needed

### After Launch

- Monitor errors and latency
- Be ready to roll back (keep previous deploy artifacts)

---

## Quick Reference: Production Env Vars

### Backend

```bash
JWT_SECRET=<256-bit secret>
DATABASE_URL=jdbc:postgresql://<host>:5432/tamixa_kids
DATABASE_USERNAME=<user>
DATABASE_PASSWORD=<pass>
SPRING_PROFILES_ACTIVE=prod
REDIS_HOST=<redis-host>
REDIS_PORT=6379
KAFKA_BOOTSTRAP_SERVERS=<kafka-brokers>  # or leave empty if not used
OPENAI_API_KEY=<key>
VOICE_ENCRYPTION_KEY=<32-byte key>
TRANSLATION_PROVIDER=openai
STORAGE_TYPE=s3
S3_BUCKET=tamixa-audio
S3_REGION=us-east-1
AWS_ACCESS_KEY_ID=<key>
AWS_SECRET_ACCESS_KEY=<secret>
CDN_STREAM_ENABLED=true
CORS_ALLOWED_ORIGINS=https://app.tamixa.com,https://admin.tamixa.com
MAGIC_LINK_BASE_URL=https://app.tamixa.com
```

### Web (build)

```bash
VITE_API_BASE_URL=https://api.tamixa.com  # if using Vite env
```

### Admin (build)

```bash
NEXT_PUBLIC_API_URL=https://api.tamixa.com
```

### Mobile (release)

- `buildConfigField("String", "BASE_URL", "\"https://api.tamixa.com\"")` in release block

---

## Files to Update

| File | Change |
|------|--------|
| `Dockerfile` | Use `backend/` context; run `:backend:bootJar` |
| `mobile/composeApp/build.gradle.kts` | Add prod BASE_URL for release |
| `mobile/androidApp/build.gradle.kts` | Same |
| `config/api.config.ts` | Support env override for prod |
| `config/api.config.json` | Or update for prod builds |
| `.github/workflows/ci.yml` | Optional: add deploy job for main |

---

## Risk Mitigation

1. **Kafka not ready:** Consider running story generation sync first; add async later.
2. **S3 Storage:** Set `S3_BUCKET`, `S3_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`; otherwise audio streaming may fail or fall back to local.
3. **Mobile Play Store:** Review can take days; use internal testing track for Sunday.
4. **Stripe/Razorpay:** Configure webhooks and test subscriptions before launch if required.
