# Getting Started — Full Stack

Get the Tamixa monorepo running locally in a few minutes.

## Prerequisites

- **Java 17+** (backend)
- **Node 18+** (web, admin, e2e)
- **PostgreSQL 16** (or Docker)
- **Redis** (or Docker)

## 1. Clone and configure

```bash
git clone <repo-url>
cd araro-kids
cp .env.example .env
# Edit .env: set DATABASE_URL, REDIS_HOST, optional API keys
```

## 2. Start infrastructure (Docker)

```bash
docker compose up -d postgres redis   # Kafka is optional: docker compose --profile kafka up -d
```

## 3. Backend

```bash
./gradlew :backend:bootRun -Ptamixa.backendOnly=true
```

- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html (open without login in the `dev` profile only; staging/prod require an admin JWT)
- Health: http://localhost:8080/actuator/health

## 4. Web (parent app)

```bash
cd web && npm ci && npm run dev
```

- App: http://localhost:3000

## 5. Admin dashboard

```bash
cd admin && npm ci && npm run dev
```

- Admin: http://localhost:3001
- Uses `NEXT_PUBLIC_API_URL=http://localhost:8080` or direct backend

## 6. Mobile (optional)

```bash
# Android (module is :mobile:composeApp; default flavor = dev)
./gradlew :mobile:composeApp:assembleDevDebug

# Set BASE_URL in mobile config to http://10.0.2.2:8080 (emulator) or your machine IP
```

Open `mobile/` in Android Studio for run and debug.

## Common commands

| Task              | Command                                                |
|-------------------|--------------------------------------------------------|
| Backend tests     | `./gradlew :backend:test -Ptamixa.backendOnly=true`   |
| Web tests         | `cd web && npm run test`                               |
| Admin tests       | `cd admin && npm test`                                 |
| E2E (full stack)  | `cd e2e && npm run test` (requires backend+web+admin)  |
| Backend-only build| `./gradlew :backend:compileKotlin -Ptamixa.backendOnly=true` |

## Environment variables

See `.env.example` for full list. Minimal local dev:

- `DATABASE_URL` or `POSTGRES_*`
- `REDIS_HOST=localhost`
- `JWT_SECRET` (any 256-bit secret)
- Optional: `OPENAI_API_KEY` and/or `GEMINI_API_KEY` (see `AI_LLM_PROVIDER`, `AI_COVER_IMAGE_PROVIDER` in `.env.example`), `GOOGLE_CLOUD_TTS_API_KEY` for narration

## Troubleshooting

- **Backend fails to start**: Ensure PostgreSQL and Redis are running; check `DATABASE_URL`.
- **Admin 401**: Log in at `/login`; JWT stored in `localStorage`.
- **Pipeline not running**: Default `PIPELINE_ON_SUBMIT_ONLY=true` — pipeline runs when you click "Submit for review" in admin. See [admin/STORY_PIPELINE_FLOW.md](admin/STORY_PIPELINE_FLOW.md).
