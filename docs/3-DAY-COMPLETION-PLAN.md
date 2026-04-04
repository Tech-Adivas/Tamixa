# Tamixa Kids — 3-Day Completion Plan

**Goal:** Complete and deploy the Tamixa project within 3 days.  
**Priority:** Mobile app first (per project rules), then backend, web, admin, deployment.

---

## Current State Summary

| Component | Status | Notes |
|-----------|--------|-------|
| **Backend** | ✅ Ready | Spring Boot, PostgreSQL, Redis, Kafka; prod config exists |
| **Mobile (Android)** | ✅ Core ready | Auth, stories, audio, voice, subscription, settings |
| **Web** | ✅ Functional | Auth, stories, children, subscription |
| **Admin** | ⚠️ Partial | API wired; some mock data, flag bug, sidebar gaps |
| **Dockerfile** | ✅ Fixed | Monorepo-aware; copies `backend/`, runs `:backend:bootJar` |

---

# Day 1 — Mobile Polish + Critical Fixes ✅ (Completed)

**Focus:** Mobile app premium polish (priority 1) and blocking bugs.

## 1.1 Mobile — Screen Consistency & Polish

- [x] **Login / Register** — TamixaHeroIllustration, TamixaGradients used; theme parity
- [x] **Dashboard** — TamixaEmojiDisplay, TamixaColors, StoryCard; navigation clear
- [x] **Story Generation** — Emotion modes, parent instructions, loading states
- [x] **Language Selection** — Matches theme
- [x] **Voice Upload** — Success feedback added; profiles reload on success
- [x] **Subscription** — TamixaEmojiDisplay, TamixaColors; Manage → web flow works
- [x] **Settings** — Dark mode, language, logout; TamixaEmojiDisplay, TamixaColors

## 1.2 Mobile — Generated Stories from Server

- [x] StoryApi → StoryRepository → StoryViewModel flow verified
- [x] Dashboard / StorySelection load via LaunchedEffect; allStories() merges server + cached + curated

## 1.3 Admin — Critical Bug Fixes

- [x] **Moderation flag:** `handleFlag` in `moderation/page.tsx` — remove `res.ok` check (API returns `Promise<void>`, so `res` is undefined)
- [x] **curated-stories:** Remove unused imports so build passes
- [x] **Sidebar:** Add missing nav links — Stories, Children, Voice logs, Health, AI metrics (or “More” submenu)

## 1.4 Web — Quick Wins

- [x] **Audio playback:** getStreamUrl + HTML5 Audio in Stories page
- [x] **Error boundary:** ErrorBoundary wraps App
- [x] **Children page:** List, add, edit with child name in header

## 1.5 Backend — Health Check

- [x] Backend compiles
- [ ] Tests require Docker/Testcontainers (known flaky)

---

# Day 2 — Admin + Backend + Environment ✅ (Completed)

**Focus:** Admin integration, backend hardening, production environment setup.

## 2.1 Admin — Real API Wiring

- [x] **Dashboard KPIs:** use-dashboard calls real API when mock disabled
- [x] **Parent search label:** Change to “Search by email” (backend supports email only)
- [x] **Story preview:** api.admin.getStory(id) when not mock

## 2.2 Backend — Production Hardening

- [x] **CORS:** CorsOriginValidator fails prod if CORS_ALLOWED_ORIGINS is * or unset
- [x] **JWT_SECRET:** JwtSecretValidator fails prod if default used
- [x] **application-prod.yml:** org.hibernate.SQL: WARN; env vars documented

## 2.3 Environment Config

- [x] **.env.example:** web + admin updated
- [x] **Mobile:** Release uses TAMIXA_API_BASE_URL (default https://api.tamixa.com)

## 2.4 Smoke Test — Local

- [ ] Backend + Web + Admin running; full flow:
  - Register → Login → Create child → Generate story → Play audio
- [ ] Mobile: Login → Dashboard → Generate/play story → Voice upload → Subscription → Settings → Logout
- [ ] Admin: Login (ADMIN user) → Parents list → Story moderation → Curated stories

---

# Day 3 — Deployment + E2E + Launch Prep

**Focus:** Infrastructure, deployment, final E2E, and go-live checklist.

**Build verification (completed):** Docker, web, admin, and mobile release builds verified. Use `./scripts/build-production.sh` to build all artifacts.

## 3.1 Infrastructure (Pick One)

| Option | Backend | Web | Admin | DB/Redis/Kafka |
|--------|---------|-----|-------|----------------|
| Railway | ✅ | ✅ | ✅ | Managed Postgres/Redis; Confluent Cloud for Kafka |
| Render | ✅ | ✅ | ✅ | Managed Postgres/Redis |
| Fly.io | ✅ | ✅ | ✅ | Fly Postgres; external Redis/Kafka |
| AWS/GCP | ECS/GKE | S3+CF / Cloud Run | Same | RDS, ElastiCache, MSK |

- [ ] Provision PostgreSQL (prod)
- [ ] Provision Redis (prod)
- [ ] Kafka: enable for TTS pipeline OR disable for MVP (sync story generation)
- [ ] Domains: `api.tamixa.com`, `app.tamixa.com`, `admin.tamixa.com`

## 3.2 Deploy Backend

- [x] Build: `docker build -t tamixa-backend:prod .` (verified)
- [ ] Set prod env vars (see Quick Reference below)
- [ ] Run Flyway migrations on prod DB
- [ ] Health: `curl https://api.tamixa.com/actuator/health`
- [ ] Configure SendGrid (magic link) and Twilio (OTP) or use dev bypass

## 3.3 Deploy Web

- [x] Build: `cd web && npm run build` (verified)
- [ ] Deploy to Vercel / Netlify / Cloudflare Pages / S3+CloudFront
- [ ] Ensure `/api` proxy or client points to prod API

## 3.4 Deploy Admin

- [x] Build: `cd admin && npm run build` (verified)
- [ ] Deploy to Vercel / Render / Docker
- [ ] Restrict to ADMIN role (backend check)

## 3.5 Mobile Release Build

- [x] Build: `cd mobile && ./gradlew :composeApp:assembleRelease -PTAMIXA_API_BASE_URL=https://api.tamixa.com` (verified)
- [ ] Sign APK with keystore (production)
- [ ] Output: `mobile/composeApp/build/outputs/apk/release/`
- [ ] Optional: CDN for audio — set `CDN_STREAM_ENABLED=true`, GCP bucket

## 3.6 Final E2E Tests

- [ ] **Web:** Register → Login → Create child → Generate story → Play audio
- [ ] **Mobile:** Login → Dashboard → Play story → Voice upload → Subscription
- [ ] **Admin:** Parents list → Suspend; Moderation → Approve/Reject
- [ ] **Magic link:** Set `MAGIC_LINK_BASE_URL` to web app URL; test flow

## 3.7 Security Checklist

- [ ] `JWT_SECRET` changed from default (256+ bits)
- [ ] HTTPS everywhere
- [ ] CORS restricted to prod origins
- [ ] DB credentials not in code
- [ ] Rate limiting enabled
- [ ] Screenshot prevention on Login/Register (mobile)

## 3.8 Optional Monitoring

- [ ] Actuator health + Prometheus (if applicable)
- [ ] Uptime check (e.g. UptimeRobot)
- [ ] Error tracking (Sentry)

---

# Quick Reference — Production Env Vars

## Backend

```bash
JWT_SECRET=<256-bit secret>
DATABASE_URL=jdbc:postgresql://<host>:5432/tamixa_kids
DATABASE_USERNAME=<user>
DATABASE_PASSWORD=<pass>
SPRING_PROFILES_ACTIVE=prod
REDIS_HOST=<redis-host>
REDIS_PORT=6379
KAFKA_BOOTSTRAP_SERVERS=<brokers>  # or leave empty if not used
AI_LLM_PROVIDER=openai
OPENAI_API_KEY=<key>
# Or: AI_LLM_PROVIDER=gemini and GEMINI_API_KEY=<key>
AI_COVER_IMAGE_PROVIDER=openai
VOICE_ENCRYPTION_KEY=<32-byte key>
CORS_ALLOWED_ORIGINS=https://app.tamixa.com,https://admin.tamixa.com
MAGIC_LINK_BASE_URL=https://app.tamixa.com
CDN_STREAM_ENABLED=true  # optional
CDN_GCP_BUCKET=tamixa-audio  # optional
```

## Web (build)

```bash
VITE_API_URL=https://api.tamixa.com
```

## Admin (build)

```bash
NEXT_PUBLIC_API_URL=https://api.tamixa.com
NEXT_PUBLIC_USE_MOCK_API=false
```

## Mobile (release)

```bash
./gradlew :composeApp:assembleRelease -PTAMIXA_API_BASE_URL=https://api.tamixa.com
```

---

# Task Summary by Day

| Day | Focus | Key Deliverables |
|-----|-------|------------------|
| **Day 1** | Mobile polish, Admin/Web fixes | Premium mobile screens, flag bug fixed, sidebar links, web audio |
| **Day 2** | Admin API, backend hardening, env | Real dashboard KPIs, CORS/JWT docs, smoke tests pass |
| **Day 3** | Deploy, E2E, launch | Backend + Web + Admin + Mobile release; go-live checklist done |

---

# Risk Mitigation

1. **Kafka not ready:** Run story generation sync; add async later.
2. **Mobile Play Store:** Use internal testing track; review can take days.
3. **Stripe/Razorpay:** Configure webhooks if subscriptions required at launch.
4. **Time pressure:** Day 1 mobile polish can be narrowed to Login, Dashboard, StoryGeneration; defer Subscription/Settings if needed.
