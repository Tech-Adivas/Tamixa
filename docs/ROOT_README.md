# Tamixa — Multi-module project

Monorepo for **Tamixa** (kids’ stories): **backend**, **mobile**, and **web**.

**Documentation:** [docs/](docs/README.md) — **[project overview & diagrams](docs/PROJECT_DOCUMENTATION.md)**, **[architecture & integrations](docs/ARCHITECTURE.md)**, **[enterprise roadmap](docs/ENTERPRISE_ROADMAP.md)** (consumer quality now, B2B later), deployment, security, naming, and feature guides. **Quick start:** [docs/GETTING_STARTED.md](docs/GETTING_STARTED.md).

**AI governance & control plane:** [AGENTS.md](AGENTS.md) (agent workflow), **[AI_GOVERNANCE_E2E.md](docs/AI_GOVERNANCE_E2E.md)** (what is wired end-to-end), **[CONTROL_PLANE_ARCHITECTURE.md](docs/CONTROL_PLANE_ARCHITECTURE.md)** (target system), Flyway **`V75__ai_control_plane`**, GitHub Actions **spec-first-guard** + **PR metrics** workflows (see [docs/METRICS_EXPORT.md](docs/METRICS_EXPORT.md)).

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

## DORA metrics automation

Use the helper script to report deployment and incident events to the admin DORA endpoints:

```bash
# Deployment success/failure (run in deploy pipeline)
DORA_API_BASE_URL=https://api.example.com \
DORA_ADMIN_BEARER_TOKEN=<admin-jwt> \
scripts/report-dora-event.sh deployment \
  --status SUCCESS \
  --service backend \
  --environment production \
  --change-id "$GITHUB_SHA"

# Incident opened/resolved (run from incident automation/runbook)
DORA_API_BASE_URL=https://api.example.com \
DORA_ADMIN_BEARER_TOKEN=<admin-jwt> \
scripts/report-dora-event.sh incident \
  --status OPEN \
  --service backend \
  --environment production \
  --summary "Elevated 5xx error rate"
```

The dashboard reads aggregate metrics from `GET /api/v1/admin/metrics/dora`.

GitHub automation options:

- Manual run: use workflow **DORA metrics report** (`.github/workflows/dora-metrics-report.yml`) from Actions tab.
- API trigger from external deploy system via `repository_dispatch`:

```bash
curl -X POST "https://api.github.com/repos/<owner>/<repo>/dispatches" \
  -H "Authorization: Bearer <github-token>" \
  -H "Accept: application/vnd.github+json" \
  -d '{
    "event_type": "dora_deployment",
    "client_payload": {
      "status": "SUCCESS",
      "service": "backend",
      "environment": "production",
      "change_id": "abc1234"
    }
  }'
```

Required GitHub repository secrets for this workflow:
- `DORA_API_BASE_URL`
- `DORA_ADMIN_BEARER_TOKEN`

From another workflow, you can also call it directly via `workflow_call`:

```yaml
jobs:
  report-dora-deployment:
    if: ${{ success() }}
    uses: ./.github/workflows/dora-metrics-report.yml
    with:
      mode: deployment
      status: SUCCESS
      service: backend
      environment: production
      change_id: ${{ github.sha }}
    secrets:
      DORA_API_BASE_URL: ${{ secrets.DORA_API_BASE_URL }}
      DORA_ADMIN_BEARER_TOKEN: ${{ secrets.DORA_ADMIN_BEARER_TOKEN }}
```

A complete starter workflow is included at `.github/workflows/deploy-backend-example.yml`.
Replace the `Deploy backend placeholder` step with your real deployment command.

Automatic reporting from workflow completion is also available via
`.github/workflows/dora-from-workflow-run.yml`. It currently listens to
`Deploy backend (example)` and posts `SUCCESS`/`FAILED` deployment events based on workflow conclusion.
If you rename or replace the deploy workflow, update the `workflow_run.workflows` list accordingly.

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
