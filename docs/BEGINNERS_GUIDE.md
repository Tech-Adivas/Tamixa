# Tamixa — Beginner's Guide: Setup, Testing & Deployment

> Audience: a new developer joining the project with no prior context.
> Written 26 Sep 2026 after a full end-to-end test of the repo (see `docs/TEST_REPORT_2026-09-26.md`).
> Every command below was either run during that test or is copied from the repo's own config files.

---

## 1. What you are setting up

Tamixa is one monorepo with five parts that talk to one backend API:

```
                 ┌──────────────────────────┐
  Mobile app ───▶│                          │──▶ PostgreSQL 16  (all data, Flyway migrations)
  (Android/iOS)  │   Backend API            │──▶ Redis 7        (rate limits, JWT logout list, cache)
  Parent web ───▶│   Kotlin + Spring Boot   │──▶ AWS S3         (audio, covers)
  (React/Vite)   │   port 8080              │──▶ OpenAI / Gemini / Google TTS / ElevenLabs / Fish Audio / HeyGen
  Admin panel ──▶│                          │──▶ SendGrid       (email codes / magic links)
  (Next.js)      └──────────────────────────┘
```

| Folder | What it is | Tech | Local port |
|---|---|---|---|
| `backend/` | REST API, story pipeline, auth | Kotlin 1.9, Spring Boot 3.2, Java 17 target | 8080 |
| `web/` | Parent-facing web app | React 18 + Vite 7 | 3000 |
| `admin/` | Content-ops dashboard | Next.js 15 + React 19 | 3001 |
| `mobile/` | Kid + parent app (primary product) | Kotlin Multiplatform, Compose, Android + iOS | — |
| `story-pipeline/` | Offline story tooling | TypeScript | — |
| `e2e/` | Browser smoke tests | Playwright | — |
| `backend/src/main/resources/db/migration/` | Database schema (V1 … V100) | Flyway SQL | — |

Kafka and Zookeeper are optional in `docker-compose.yml` (profile `kafka`) — the backend publishes events inline today. Start them only if needed: `docker compose --profile kafka up -d`.

---

## 2. Install the tools (one time, on your Mac)

| Tool | Version | Why | Check |
|---|---|---|---|
| Git | any | clone code | `git --version` |
| **JDK 17 or 21** (Temurin) | 17+ | backend + Android build | `java -version` |
| **Node.js** | 20.19+ LTS (Vite 7 / Next 15 need it) | web, admin, e2e | `node -v` |
| **Docker Desktop** | latest | Postgres + Redis locally | `docker ps` |
| **Android Studio** | latest (AGP 8.9.3, SDK 35) | mobile app | open `mobile/` |
| Xcode | latest | only for iOS | — |
| `psql` client (optional) | 16+ | look inside the DB | `psql --version` |

Tips:
- Install JDK with `brew install --cask temurin@17` or SDKMAN (`sdk install java 17.0.x-tem`).
- Give Docker Desktop at least 4 GB RAM.

---

## 3. Get the code and the config

```bash
git clone <repo-url> araro-kids
cd araro-kids
cp .env.example .env          # backend + docker-compose read this file
cp admin/.env.example admin/.env.local
cp web/.env.example web/.env  # optional; web defaults to http://localhost:8080
```

### 3.1 Minimum `.env` to start locally (no paid AI keys needed)

```dotenv
DATABASE_URL=jdbc:postgresql://localhost:5432/araro_kids
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres
REDIS_HOST=localhost
JWT_SECRET=change-me-to-a-long-random-string-of-at-least-32-characters
TRANSLATION_PROVIDER=simulated
```

This exact minimal set was tested: the backend starts, applies all 100 migrations and reports healthy with **no** AI or AWS keys. Features that need a provider (translation, narration, S3 audio, voice cloning, avatars) fail only when used, until you add keys.

### 3.2 Keys for the real AI features (add when you need them)

| Feature | Variables |
|---|---|
| Story text / rewrite | `AI_LLM_PROVIDER` (`openai` or `gemini`) + `OPENAI_API_KEY` / `GEMINI_API_KEY` |
| Translation | `TRANSLATION_PROVIDER=openai|gemini` |
| Narration (voice) | `NARRATION_TTS_PROVIDER=google`, `GOOGLE_CLOUD_TTS_API_KEY` |
| Cover images / animation | `AI_COVER_IMAGE_PROVIDER`, `COVER_ANIMATION_VEO_ENABLED` |
| Voice cloning | `VOICE_CLONING_ENABLED`, `FISH_AUDIO_API_KEY`, `ELEVENLABS_API_KEY` |
| Avatar video | `AVATAR_VIDEO_ENABLED`, `HEYGEN_API_KEY` (or D-ID / Gooey) |
| Audio storage | `STORAGE_TYPE=s3`, `S3_BUCKET`, `S3_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` |
| Email codes | `SENDGRID_API_KEY` |

Full list: `.env.example` and `docs/ENV_REFERENCE.md`.

**Rules:** `.env` is git-ignored — never commit it, never paste keys in Slack/PRs. `S3_BUCKET` can be a bucket name (`tamixa-audio`) or an S3 Access Point ARN. If a variable appears twice in `.env`, the last one wins — keep one copy.

---

## 3.5 Quick start — everything with one command

Double-click **Start Tamixa.command** in the repo folder (or run `./scripts/run-all.sh`). It starts PostgreSQL + Redis (Homebrew or Docker Desktop), the backend (dev profile, seeds the dev admin), web on :3000 and admin on :3001, installs npm packages when they changed, and opens the admin login. Logs: `logs/run/*.log`. Stop with **Stop Tamixa.command** (`./scripts/stop-all.sh`) — Postgres/Redis keep running.

If admin says **"Backend unreachable"**, the backend is not running, or the Docker compose `web`/`admin` containers are holding ports 3000/3001 (they point at an in-Docker backend). `run-all.sh` stops those containers for you; manually: `docker compose stop web admin backend`.

Sections 4–7 below explain the same steps one by one.

## 4. Start the database and Redis

```bash
docker compose up -d postgres redis
docker compose ps            # both should show "healthy"
```

- Postgres: `localhost:5432`, user `postgres`, password `postgres`, database `araro_kids`
- Redis: `localhost:6379`

You do **not** create tables by hand. The backend runs all Flyway migrations (V1 → V100) on first start.

Check the DB any time:

```bash
psql postgresql://postgres:postgres@localhost:5432/araro_kids -c "select count(*) from flyway_schema_history;"
```

---

## 5. Run the backend

```bash
./gradlew :backend:bootRun -Ptamixa.backendOnly=true
```

`-Ptamixa.backendOnly=true` skips the mobile module, so you don't need the Android SDK for backend work. The first run downloads dependencies (5–10 min). Startup takes ~40 s.

**You are good when you see:** `Successfully applied 100 migrations` and `Started TamixaApplicationKt`.

Verify:

```bash
curl http://localhost:8080/actuator/health          # {"status":"UP",...}
curl -X POST http://localhost:8080/api/v1/dev/seed-admin
#   → creates admin@techadivas.com / Admin123!  (dev profile only)
```

Useful dev-only helpers (dev profile):

| What | How |
|---|---|
| Admin login | `admin@techadivas.com` / `Admin123!` after `seed-admin` |
| Phone OTP login | any phone + code `DEV_OTP_CODE` (default `123456`) |
| Email code login (web) | any email + code `123456` |
| Check all AI/S3 keys at once | `GET /api/v1/dev/verify-connections` |
| Swagger | `http://localhost:8080/swagger-ui.html` — open without login in the `dev` profile; staging/prod need an admin JWT |

> Timezone note (India): older JVMs report `Asia/Calcutta`, which Postgres rejects. Since 26 Sep 2026 the backend converts it to `Asia/Kolkata` automatically at startup (log line: `JVM default time zone Asia/Calcutta normalised to Asia/Kolkata`).

Backend tests:

```bash
./gradlew :backend:test -Ptamixa.backendOnly=true
```

---

## 6. Run the parent web app

```bash
cd web
npm ci
npm run dev           # http://localhost:3000
```

Log in: enter any email → **Send sign-in code** → type `123456` → tick the Terms / Privacy / Parent checkboxes (first time only) → sign in.

Checks:

```bash
npm run lint
npm test
npm run build
```

---

## 7. Run the admin dashboard

```bash
cd admin
npm ci
npm run dev           # http://localhost:3001
```

`admin/.env.local` needs:

```dotenv
API_URL=http://localhost:8080
NEXT_PUBLIC_API_URL=http://localhost:8080
```

Open http://localhost:3001 → **Admin login** → `admin@techadivas.com` / `Admin123!`.

Checks:

```bash
npm run lint
npx tsc --noEmit      # type check (next build also does this)
npm test
npm run build
```

### How content gets to users (important for beginners)

The database starts with 70 seeded library stories, but they are all in **DRAFT**. Parents and the mobile app only see **PUBLISHED** stories. The flow in admin is:

`DRAFT → Submit for review → TRANSLATING → CONTENT_REVIEW → Approve → AUDIO_GENERATING → AUDIO_REVIEW → PUBLISHED`

Translation and audio steps call the AI/TTS providers, so they need real keys (section 3.2). Details: `docs/admin/STORY_PIPELINE_FLOW.md`.

---

## 8. Run the mobile app

### 8.1 Android

1. Open the **`mobile/`** folder in Android Studio (it has its own Gradle wrapper and settings).
2. Tell the app where your backend is — create/edit `mobile/local.properties`:
   ```properties
   sdk.dir=/Users/<you>/Library/Android/sdk
   # Emulator talks to your Mac through 10.0.2.2 (this is also the default)
   TAMIXA_API_BASE_URL=http://10.0.2.2:8080
   # Real phone on same Wi-Fi: use your Mac's LAN IP, e.g. http://192.168.0.9:8080
   ```
3. Pick build variant **devDebug** and press Run. Or from the terminal (task names come from `mobile/composeApp/build.gradle.kts`; the Android build could not be executed in the 26 Sep test run — see the test report):
   ```bash
   cd mobile
   ./gradlew :composeApp:assembleDevDebug          # build APK
   ./gradlew :composeApp:installDevDebug           # install on running emulator/phone
   ./gradlew :composeApp:testDevDebugUnitTest      # shared unit tests
   ```
   From the repo root the same module is `:mobile:composeApp:…` (CI runs `./gradlew :mobile:composeApp:compileDevDebugKotlinAndroid`).
4. Log in with any phone number and OTP `123456`.

Flavors: `dev` (local API), `qa` (`-PTAMIXA_QA_API_BASE_URL=…`), `prod` (`-PTAMIXA_API_BASE_URL=…`).

When testing on a real phone, also set `AUDIO_PUBLIC_BASE_URL=http://<your-LAN-IP>:8080` in the backend `.env` so audio links are reachable from the phone.

### 8.2 iOS (optional)

Open `mobile/iosApp/Tamixa.xcodeproj` in Xcode; it builds the shared Kotlin framework via `build-framework.sh`. See `docs/mobile/ios-README.md` and `docs/mobile/IOS_TESTING.md`.

---

## 9. Testing checklist (run before every release)

### 9.1 Automated

| Area | Command | Expected |
|---|---|---|
| Migrations sanity | `bash .github/scripts/flyway-duplicate-version-guard.sh` | `OK` |
| Backend unit/integration | `./gradlew :backend:test -Ptamixa.backendOnly=true` | BUILD SUCCESSFUL |
| Web | `cd web && npm run lint && npm test && npm run build` | all green |
| Admin | `cd admin && npm run lint && npx tsc --noEmit && npm test && npm run build` | all green |
| Story pipeline | `cd story-pipeline && npm run typecheck && npm run build` | no errors |
| Android compile | `./gradlew :mobile:composeApp:compileDevDebugKotlinAndroid` | BUILD SUCCESSFUL |
| Browser smoke | backend + web + admin running, then `cd e2e && npx playwright install chromium && npm test` | all pass |
| Security audit | `npm audit --audit-level=critical` in `web/` and `admin/` | exit 0 (CI fails otherwise) |

### 9.2 Manual smoke (15 minutes)

| # | Step | Expected |
|---|---|---|
| 1 | `GET /actuator/health` | `UP` |
| 2 | Admin: log in, open Dashboard, Story library, Parents, Health | pages load, no red error overlay |
| 3 | Admin: open a DRAFT story → Submit for review | status moves to TRANSLATING then CONTENT_REVIEW |
| 4 | Admin: Approve → Generate audio → Publish | status PUBLISHED |
| 5 | Web: log in with email code, open Stories | the published story is listed |
| 6 | Web: play the story | audio plays |
| 7 | Mobile: log in with OTP, open Library, play the same story | works on emulator |
| 8 | Log out on web, then reuse the old token | `401` (token revoked in Redis) |

### 9.3 Database checks

```sql
-- migrations all applied?
select count(*), bool_and(success) from flyway_schema_history;
-- story status overview
select status, count(*) from library_stories group by 1 order by 2 desc;
-- users
select id, email, role from parents order by id desc limit 10;
```

Redis: `redis-cli --scan --pattern 'auth:revoked:*' | head` shows logged-out tokens.

---

## 10. Deploying (Railway — what this repo is already set up for)

The repo already contains Railway-friendly Dockerfiles and env guards. Canonical dev hosts from `railway.env.example`: API `dev.tamixa.in`, web `dev.web.tamixa.in`, admin `dev.admin.tamixa.in`. Other options (AWS ECS, Render, Fly) are in `docs/DEPLOYMENT_PLAN.md` and `docs/DEPLOYMENT_PLAN_AWS.md`.

### 10.1 One-time setup

1. Create a Railway project.
2. Add **PostgreSQL** and **Redis** plugins.
3. Create three services from the GitHub repo:

| Service | Root directory | Dockerfile | Port |
|---|---|---|---|
| backend | `/` (repo root) | `Dockerfile` | 8080 |
| web | `web` | `web/Dockerfile` | 3000 (`$PORT`) |
| admin | `admin` | `admin/Dockerfile` | 3000 |

### 10.2 Backend variables (production)

| Variable | Value |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` (or `staging`) |
| `DATABASE_URL` / `DATABASE_USERNAME` / `DATABASE_PASSWORD` | from the Railway Postgres plugin (the app converts `postgresql://…` to JDBC automatically) |
| `REDIS_HOST`, `REDIS_PORT` | from the Redis plugin |
| `JWT_SECRET` | new random 64+ chars (never the default) |
| `CORS_ALLOWED_ORIGINS` | `https://<web-domain>,https://<admin-domain>` |
| `MAGIC_LINK_BASE_URL`, `TAMIXA_WEB_APP_URL` | web domain |
| `VOICE_ENCRYPTION_KEY` | 32-byte Base64 (`openssl rand -base64 32`) |
| `AUDIO_PUBLIC_BASE_URL` | API public URL |
| AI / S3 / SendGrid keys | as in section 3.2 |
| `SEED_ADMIN_ENABLED` | `false` |
| `DEV_OTP_CODE`, `DEV_PASSWORDLESS_CODE` | **unset** |

Flyway runs automatically on startup. Pending migrations must be applied before the new version serves traffic (see `docs/DEPLOYMENT_CHECKLIST.md`).

Create the first real admin in production through a controlled SQL/ops step — not `seed-admin`.

### 10.3 Web variables

`VITE_API_BASE_URL=https://<api-domain>` — this is baked in at **build** time, so redeploy after changing it.

### 10.4 Admin variables

`API_URL=https://<api-domain>` and `NEXT_PUBLIC_API_URL=https://<api-domain>` (no trailing slash). `NEXT_PUBLIC_*` is baked in at build time — redeploy after changes.

### 10.5 After each deploy

```bash
curl https://<api-domain>/actuator/health
```
Then run the manual smoke in section 9.2 against the deployed URLs, or `WEB_BASE_URL=… ADMIN_BASE_URL=… npx playwright test` from `e2e/`.

### 10.6 Android release (Play Store)

1. Create an upload keystore once (`keytool -genkeypair -v -keystore tamixa-upload.jks -alias tamixa -keyalg RSA -keysize 2048 -validity 10000`). Store it and its passwords outside git.
2. Put the signing values in `mobile/local.properties` (or CI env) — `mobile/composeApp/build.gradle.kts` reads them; without them the release build stays unsigned:
   ```properties
   TAMIXA_UPLOAD_STORE_FILE=/Users/<you>/keys/tamixa-upload.jks
   TAMIXA_UPLOAD_STORE_PASSWORD=...
   TAMIXA_UPLOAD_KEY_ALIAS=tamixa
   TAMIXA_UPLOAD_KEY_PASSWORD=...
   ```
   Check with `./gradlew :composeApp:signingReport` → `prodRelease` shows your keystore.
3. Build the bundle:
   ```bash
   cd mobile
   ./gradlew :composeApp:bundleProdRelease \
     -PTAMIXA_API_BASE_URL=https://<api-domain> \
     -PTAMIXA_WEB_APP_URL=https://<web-domain>
   ```
4. Upload `composeApp/build/outputs/bundle/prodRelease/*.aab` to Play Console → Internal testing first.
5. Bump `versionCode` / `versionName` for every upload.

### 10.7 Database backup / restore

```bash
pg_dump -Fc "$DATABASE_PUBLIC_URL" > tamixa-$(date +%F).dump
pg_restore --no-owner -d araro_kids tamixa-YYYY-MM-DD.dump
```
`pg_restore` must be the same or newer major version than the `pg_dump` that made the file. The existing `tamixa.local.dump` was made with pg_dump **18**, so a Postgres 16 client cannot read it. More: `docs/runbooks/BACKUP_AND_RESTORE.md`.

---

## 11. Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `Connection refused` to 5432 on backend start | Postgres not running | `docker compose up -d postgres redis` |
| `invalid value for parameter "TimeZone": "Asia/Calcutta"` | Old IANA name from the JVM | fixed in code since 26 Sep; on older builds use `-Duser.timezone=Asia/Kolkata` |
| Swagger / `/v3/api-docs` returns 401 | Not running the `dev` profile | use `SPRING_PROFILES_ACTIVE=dev`, or call with `Authorization: Bearer <admin token>` |
| Register returns 400 "consent required" | New accounts must accept Terms + Privacy + Parent attestation | send `acceptedTerms`, `acceptedPrivacy`, `acceptedParentalAttestation: true` |
| 429 "Rate limit exceeded" on login | Auth endpoints are rate-limited per IP | wait ~1 minute |
| Web/mobile library is empty | Seeded stories are DRAFT | publish a story from admin (section 7) |
| Admin page shows red "Build Error" overlay | A page imports a missing file | fix the import and reload (in dev the overlay then blocks every other page too) |
| 403 "Access denied" | Logged-in user lacks the role (e.g. parent calling `/admin/**`) | use the right account |
| 400 "Unknown story status" | Wrong `status` filter on `/admin/stories` | use unified names: DRAFT, SUBMITTED, TRANSLATING, TRANSLATION_FAILED, CONTENT_REVIEW, CHANGES_REQUESTED, REJECTED, APPROVED, AUDIO_GENERATING, AUDIO_FAILED, AUDIO_REVIEW, PUBLISHED (comma list allowed) |
| Admin 401 / redirect to login | JWT expired (15 min access token) | log in again |
| Admin "503 / API unreachable" | `API_URL` wrong | set `API_URL` and `NEXT_PUBLIC_API_URL` to the backend origin |
| Mobile can't reach API | Using `localhost` on emulator | use `http://10.0.2.2:8080`, or LAN IP on a phone |
| Audio doesn't play on phone | `AUDIO_PUBLIC_BASE_URL` points to localhost | set it to your LAN IP / public API URL |
| Story stuck in TRANSLATING | AI key missing / provider down | `GET /api/v1/dev/verify-connections`, then admin → Pipeline triage |
| `pg_restore: unsupported version (1.16)` | Dump made by newer pg_dump | use a Postgres 18 client |

---

## 12. Where to read next

- `docs/ARCHITECTURE.md`, `docs/TAMIXA_ARCHITECTURE.md` — the big picture
- `docs/admin/STORY_PIPELINE_FLOW.md` — how stories move through statuses
- `docs/ENV_REFERENCE.md` — every environment variable
- `docs/DEPLOYMENT_PLAN.md`, `docs/DEPLOYMENT_CHECKLIST.md` — deeper deploy detail
- `AGENTS.md` — coding rules and review checklist
- `docs/TEST_REPORT_2026-09-26.md` — what was tested, what was fixed, what is still open
- `docs/archive/` — old one-off fix notes moved from the repo root; ad-hoc SQL moved to `scripts/sql/`
