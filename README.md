# Tamixa — Multi-module project

Monorepo for **Tamixa** (kids’ stories): **backend**, **mobile**, and **web**.

**Documentation:** [docs/](docs/README.md) — architecture, deployment, security, naming conventions, and feature guides. **Quick start:** [docs/GETTING_STARTED.md](docs/GETTING_STARTED.md).

```
tamixa/
├── backend/     # Spring Boot API (Kotlin)
├── mobile/      # Kotlin Multiplatform app (Android; iOS-ready)
├── web/         # React + Vite + TypeScript frontend
├── build.gradle.kts
└── settings.gradle.kts
```

## Modules

| Module   | Stack                    | Run / Build |
|----------|--------------------------|-------------|
| **backend** | Spring Boot 3, Kotlin, PostgreSQL, Redis | `./gradlew :backend:bootRun` |
| **admin**   | Next.js 14, Tailwind, Radix UI | `cd admin && npm run dev` (port 3001) |
| **mobile**  | KMP, Compose, Ktor, Koin | `./gradlew :mobile:androidApp:assembleDebug` |
| **web**     | React, Vite, TypeScript  | `./gradlew :web:npm_run_dev` or `cd web && npm run dev` |

## Quick start

1. **Gradle wrapper** is included. Use `./gradlew` from the project root.

2. **Backend** (needs PostgreSQL, Redis; e.g. Docker):
   ```bash
   ./gradlew :backend:bootRun
   ```
   For backend-only runs (e.g. when mobile is not needed), use `-Ptamixa.backendOnly=true`; see [docs/GETTING_STARTED.md](docs/GETTING_STARTED.md).
   API: `http://localhost:8080`, Swagger: `/swagger-ui.html`, base path: `/api/v1`.

   **Backend-only build** (skip mobile; useful when Android SDK is not installed or for CI):
   ```bash
   ./gradlew :backend:compileKotlin -Ptamixa.backendOnly=true
   # Or use the helper script:
   ./scripts/build-backend.sh
   ```

3. **Web** (proxies `/api` to backend):
   ```bash
   ./gradlew :web:npm_run_dev
   ```
   Or: `cd web && npm ci && npm run dev` → `http://localhost:3000`.

4. **Mobile** (Android):
   ```bash
   ./gradlew :mobile:androidApp:assembleDebug
   ```
   Open `mobile/` in Android Studio for run/config. Set `BASE_URL` to your API.

## Backend (Beta-ready)

- **Java 17+**, **Kotlin 1.9**, **Spring Boot 3.2** — Web, Security, JPA, Validation, Actuator, Redis.
- **Architecture**: domain, application, infrastructure, api (layered; no Kafka; ThreadPoolExecutor for async).
- **Config**: `backend/src/main/resources/application.yml`; profiles: `dev`, `staging`, `prod`.
- **API versioning**: all REST under `/api/v1` (e.g. `/api/v1/auth/register`, `/api/v1/children`).
- **Tests**: `./gradlew :backend:test` (Testcontainers for Postgres).

See `backend/` and [docs/PRODUCTION_UPGRADE.md](docs/PRODUCTION_UPGRADE.md) for details.

## Mobile

Kotlin Multiplatform app: auth, child profiles, story generation, audio, voice upload, subscription, settings.  
See `mobile/README.md` and [docs/mobile/FOLDER_STRUCTURE.md](docs/mobile/FOLDER_STRUCTURE.md).

## Web

Vite + React + TypeScript; dev server proxies `/api` to the backend.  
See `web/package.json` for scripts; extend with auth and API client as needed.
