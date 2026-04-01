# DORA Metrics Framework

This document explains the DORA implementation in Tamixa: what it measures, how data is collected, and how to operate it in day-to-day delivery.

---

## 1) What DORA means

DORA (DevOps Research and Assessment) tracks four core software delivery metrics:

1. **Deployment Frequency**: how often successful deployments happen.
2. **Lead Time for Changes**: time from change start to deployment.
3. **Change Failure Rate**: percentage of deployments that fail.
4. **MTTR (Mean Time to Restore)**: average time to recover from incidents.

Together, these measure delivery speed and reliability at the same time.

---

## 2) Why this matters for Tamixa

Tamixa has multiple critical user-facing paths (story generation, narration, translation, playback). DORA gives an objective way to answer:

- Are releases happening regularly?
- Are release failures increasing?
- How quickly do we recover from incidents?
- Is delivery speed improving without stability regressions?

This helps product and engineering make decisions using trend data instead of anecdotes.

---

## 3) Implementation in this repository

### Backend data model

- Table: `dora_metric_events`
- Migration: `backend/src/main/resources/db/migration/V79__dora_metric_events.sql`
- Entity: `backend/src/main/kotlin/com/tamixa/infrastructure/persistence/DoraMetricEventEntity.kt`
- Repository: `backend/src/main/kotlin/com/tamixa/infrastructure/persistence/DoraMetricEventJpaRepository.kt`

Two event types are stored:

- `DEPLOYMENT` (`SUCCESS` / `FAILED`)
- `INCIDENT` (`OPEN` / `RESOLVED`)

### Metric aggregation service

- Service: `backend/src/main/kotlin/com/tamixa/application/analytics/DoraMetricsService.kt`
- Computes:
  - successful and failed deployments
  - deployment frequency/day
  - change failure rate (%)
  - lead time p50/p95
  - MTTR
  - open/resolved incidents

### Admin API

Controller: `backend/src/main/kotlin/com/tamixa/api/controller/AdminController.kt`

- `GET /api/v1/admin/metrics/dora`
  - Query params: `days`, `serviceName`, `environment`
- `POST /api/v1/admin/metrics/dora/deployments`
- `POST /api/v1/admin/metrics/dora/incidents`

DTOs:

- `backend/src/main/kotlin/com/tamixa/api/admin/dto/DoraMetricsDto.kt`
- `backend/src/main/kotlin/com/tamixa/api/admin/dto/RecordDoraDeploymentRequest.kt`
- `backend/src/main/kotlin/com/tamixa/api/admin/dto/RecordDoraIncidentRequest.kt`
- `backend/src/main/kotlin/com/tamixa/api/admin/dto/DoraMetricEventResponseDto.kt`

### Admin dashboard

- UI page: `admin/src/app/(dashboard)/dashboard/monitoring/page.tsx`
- API client: `admin/src/lib/api.ts`
- Types: `admin/src/types/api.ts`

The monitoring page shows DORA cards and supports filter persistence for:

- days
- service
- environment

Preferences are saved per browser session in `localStorage`.

---

## 4) Event ingestion options

### Option A: script (local or CI/CD job)

Script: `scripts/report-dora-event.sh`

Env vars:

- `DORA_API_BASE_URL`
- `DORA_ADMIN_BEARER_TOKEN`

Examples:

```bash
# Deployment
scripts/report-dora-event.sh deployment \
  --status SUCCESS \
  --service backend \
  --environment production \
  --change-id "$GITHUB_SHA"

# Incident opened
scripts/report-dora-event.sh incident \
  --status OPEN \
  --service backend \
  --environment production \
  --summary "Elevated 5xx error rate"
```

### Option B: GitHub Actions reusable workflow

- Workflow: `.github/workflows/dora-metrics-report.yml`
- Supports:
  - `workflow_dispatch`
  - `repository_dispatch`
  - `workflow_call`

Required repository secrets:

- `DORA_API_BASE_URL`
- `DORA_ADMIN_BEARER_TOKEN`

### Option C: deploy workflow example

- Example: `.github/workflows/deploy-backend-example.yml`
- Emits DORA deployment `SUCCESS`/`FAILED` based on deploy job outcome.

### Option D: workflow completion hook

- Workflow: `.github/workflows/dora-from-workflow-run.yml`
- Listens to completed deploy workflows and reports deployment status automatically.

---

## 5) Authentication model

There is no separate "DORA API key" in this implementation.

- `DORA_ADMIN_BEARER_TOKEN` is an **admin JWT** from Tamixa auth (`/api/v1/auth/login`).
- Keep token handling in GitHub Secrets or secure secret storage.
- Rotate tokens periodically and after incident response events.

---

## 6) Operational playbook

### Minimum setup checklist

- [ ] Run migration `V79__dora_metric_events.sql` in target environment.
- [ ] Configure secrets `DORA_API_BASE_URL` and `DORA_ADMIN_BEARER_TOKEN`.
- [ ] Enable one ingestion path (script or workflow automation).
- [ ] Confirm admin monitoring page displays non-zero values after first events.

### Suggested team routine

- Post deployment event on every production deploy result.
- Post incident `OPEN` when user-visible disruption starts.
- Post incident `RESOLVED` when service is restored.
- Review trends weekly:
  - Deployment Frequency
  - Change Failure Rate
  - MTTR

---

## 7) Validation and test coverage

- Service tests:
  - `backend/src/test/kotlin/com/tamixa/application/analytics/DoraMetricsServiceTest.kt`
- Covers:
  - aggregate metric calculations
  - input validation (incident resolution timestamps)
  - normalization and metric counter updates

---

## 8) Current limitations and future improvements

Current:

- Deployment events depend on external workflow/reporting integration.
- Incident lifecycle is event-based; no dedicated incident domain object yet.

Recommended next:

- Add environment-specific workflow mapping in `dora-from-workflow-run.yml`.
- Add alerting on DORA trend regressions (e.g., CFR spike, MTTR spike).
- Add monthly export endpoint for BI/reporting.

