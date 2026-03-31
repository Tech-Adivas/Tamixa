# Tamixa — Backend

Spring Boot 3 API for Tamixa (auth, children, stories, voice). Part of the multi-module repo; see the [root README](../README.md) for overview.

## Run

From repo root:

```bash
./gradlew :backend:bootRun
```

Or from this directory:

```bash
../gradlew bootRun
```

## Config

- `src/main/resources/application.yml` — base config
- Profiles: `dev`, `staging`, `prod`
- API base path: `/api/v1`
- Datasource precedence:
  - `dev`: `DATABASE_URL` / `DATABASE_USERNAME` / `DATABASE_PASSWORD` (preferred), fallback to `POSTGRES_*`
  - `staging`/`prod`: `DATABASE_*` expected
  - If both `DATABASE_*` and `POSTGRES_*` are set, keep them aligned to avoid connecting different runs to different DBs.

### Flyway lock strategy (important for scale)

- `dev`: Flyway stays enabled by default (`FLYWAY_ENABLED=true`).
- `staging/prod`: Flyway is disabled by default on app nodes (`FLYWAY_ENABLED=false`) to avoid migration lock contention during multi-instance startup.
- Run migrations in exactly one dedicated release/migration job by setting `FLYWAY_ENABLED=true` for that job only.

If startup fails with `Flyway advisory lock`, identify the lock holder session:

```sql
SELECT
  a.pid,
  a.usename,
  a.application_name,
  a.state,
  a.wait_event_type,
  a.wait_event,
  now() - a.query_start AS running_for,
  LEFT(a.query, 200) AS query
FROM pg_locks l
JOIN pg_stat_activity a ON a.pid = l.pid
WHERE l.locktype = 'advisory'
  AND l.database = (SELECT oid FROM pg_database WHERE datname = current_database())
ORDER BY a.query_start;
```

### CI/CD template: single migration runner

Use one dedicated migration job, then deploy app nodes in app-only mode.

```yaml
name: Deploy Backend

jobs:
  migrate-db:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: 21
          distribution: temurin
      - name: Run DB migrations (single runner)
        env:
          SPRING_PROFILES_ACTIVE: prod
          FLYWAY_ENABLED: "true"
          DATABASE_URL: ${{ secrets.DATABASE_URL }}
          DATABASE_USERNAME: ${{ secrets.DATABASE_USERNAME }}
          DATABASE_PASSWORD: ${{ secrets.DATABASE_PASSWORD }}
        run: ./gradlew :backend:flywayMigrate --no-daemon

  deploy-app:
    needs: migrate-db
    runs-on: ubuntu-latest
    steps:
      - name: Deploy app nodes
        env:
          SPRING_PROFILES_ACTIVE: prod
          FLYWAY_ENABLED: "false"
        run: echo "Deploy your backend service/containers here"
```

Notes:
- Keep exactly one `FLYWAY_ENABLED=true` runner during rollout.
- All horizontally scaled app nodes should run with `FLYWAY_ENABLED=false`.

## Narration pipeline (library stories)

The translation + TTS pipeline runs via `StoryProcessingService.processSync()`. **Entry points**:

| Action | Endpoint | When pipeline runs |
|--------|----------|---------------------|
| Approve for delivery | `POST /api/v1/admin/stories/{id}/approve-narration` | Always |
| Trigger pipeline | `POST /api/v1/admin/stories/{id}/trigger-pipeline` | Manual recovery |
| Republish | `POST /api/v1/admin/stories/{id}/republish` | Always |
| Create story | (via `InlineStoryLibraryEventPublisher`) | Only when `PIPELINE_ON_SUBMIT_ONLY=false` |

**Default**: `PIPELINE_ON_SUBMIT_ONLY=true` — pipeline runs **only when** "Submit for review" is used, not on create or publish.

**View pipeline logs:** In dev, logs go to **console** (the terminal where you run bootRun) and to `backend/logs/tamixa.log`. Watch either. Run `./backend/scripts/check-pipeline-trigger.sh` to tail the file, or just watch the backend terminal. Look for `PIPELINE >>> RECEIVED retry storyId=X` when you click Run pipeline.

**Restart required**: After changing narration/TTS config (e.g. `NARRATION_TTS_PROVIDER`, `perSegmentTts`, `GOOGLE_CLOUD_TTS_API_KEY`), restart the backend to pick up changes.

**Tamil cloned voice (default: ElevenLabs)**: Set in backend `.env`: `VOICE_CLONING_ENABLED=true`, `VOICE_CLONING_PROVIDER=elevenlabs` (default), `ELEVENLABS_API_KEY=<your-key>`. Then: (1) Upload reference audio (admin: POST `/api/v1/admin/parents/{parentId}/voice/upload`), (2) Run job (POST `/api/v1/admin/parents/{parentId}/voice/{voiceProfileId}/run-job`), (3) Wait ~1 min, (4) Try preview again. For Google: use `VOICE_CLONING_PROVIDER=google`, `GOOGLE_CLOUD_TTS_API_KEY`, and upload consent at `.../consent` before running.

**Pipeline taking hours/days?** Root cause was single-threaded TTS: all stories shared one thread. Fix: `TRANSLATION_PIPELINE_TTS_POOL_SIZE=4` (default). Increase to 6–8 if TTS API allows more concurrency (`app.narration.max-concurrent-tts`). Use **Pipeline triage** in admin to retry failed stories.

## Tests

```bash
./gradlew :backend:test
```

Requires Docker for Testcontainers (PostgreSQL, Kafka).
