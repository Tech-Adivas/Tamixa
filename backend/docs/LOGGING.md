# Logging Guide

This document describes the logging setup, conventions, and security standards for the Tamixa backend.

## Configuration

- **Logback**: `src/main/resources/logback-spring.xml`
- **Levels**: `application.yml` (base), `application-dev.yml`, `application-prod.yml` (overrides)
- **Profiles**: `dev` (console + file, DEBUG for com.tamixa), `prod`/`staging` (JSON to console for SIEM), `test` (compact)

## MDC (Mapped Diagnostic Context)

Request-scoped fields automatically included in log output:

| Key | Source | Description |
|-----|--------|-------------|
| `traceId` | `RequestTracingFilter` | From `X-Correlation-Id` or `X-Request-Id` if valid, else new UUID; both echoed on response |
| `masterStoryId` | `StoryContextMdcFilter` | Parsed from path (e.g. `/stories/42/...`) |
| `language` | `StoryContextMdcFilter` | From query param `language` |

Use these to correlate logs across services and filter by story or request.

## Log Levels

| Level | Use when |
|-------|----------|
| **INFO** | Business events (story created, narration approved, subscription updated, admin action) |
| **WARN** | Recoverable issues (retry, fallback, S3 delete failed, validation warning) |
| **ERROR** | Failures requiring attention; include stack trace when useful |
| **DEBUG** | Development detail (SQL, internal state); disabled in prod |

## Structured Logging

Prefer parameterized logging for traceability:

```kotlin
// ✅ GOOD – structured, queryable
log.info("Narration approved for story id={} language={}", masterStoryId, effectiveLang)
log.warn("S3 delete failed for key={}: {}", key, e.message)

// ❌ BAD – string concatenation, no structured fields
log.info("Narration approved for story " + id)
```

## PII and Secrets

**Never log:** passwords, tokens, API keys, full child names, full emails, full phone numbers.

Use `PiiMask` from `com.tamixa.infrastructure.logging`:

```kotlin
import com.tamixa.infrastructure.logging.PiiMask

log.info("Login attempt email={} success={}", PiiMask.maskEmail(email), success)
log.debug("Token validation failed: {}", PiiMask.maskToken(token))
log.info("Child created name={} parentId={}", PiiMask.maskChildName(name), parentId)
```

## Environment Overrides

Set these env vars to tweak logging without code changes:

| Variable | Default | Description |
|----------|---------|-------------|
| `LOG_LEVEL_ROOT` | `INFO` | Root logger level |
| `LOG_LEVEL_COM_TAMIXA` | `DEBUG` (dev) / `INFO` (prod) | Application packages |
| `LOG_LEVEL_HIBERNATE_SQL` | `WARN` (default) / `DEBUG` when needed | Hibernate SQL; default `WARN` hides SELECT/INSERT; set `DEBUG` to debug SQL |
| `LOG_PATH` | `logs` | Directory for file appender |

Example: `LOG_LEVEL_HIBERNATE_SQL=DEBUG ./gradlew :backend:bootRun` to enable SQL logging when debugging.

## Audit Logging

Security/compliance events go to the `AUDIT` logger (JSON in prod):

- Login attempts, OTP, story generation, voice upload/delete
- Subscription changes, admin actions, data export requests

See `StructuredAuditLogger` and `AuditLogPort`.

## Pipeline Logs

Pipeline (translate + TTS) logs use the prefix **`PIPELINE >>>`** for easy filtering.

**Log file location:** In dev, logs go to **both console and file**.

- **Console:** Watch the terminal where you run `./gradlew :backend:bootRun` — all pipeline logs appear there.
- **File:** `logs/tamixa.log` (relative to process working directory). When running from repo root, Gradle often uses backend as working dir, so the file is usually `backend/logs/tamixa.log`. Run `./backend/scripts/check-pipeline-trigger.sh` to find and tail it.

Override with env: `LOG_PATH=/var/log/tamixa` (absolute or relative).

**View pipeline logs:**

```bash
# Tail pipeline lines only
tail -f backend/logs/tamixa.log | grep "PIPELINE >>>"

# Or use the helper script
./backend/scripts/check-pipeline-trigger.sh
```

**Key pipeline log lines:**

| Log | Meaning |
|-----|---------|
| `PIPELINE >>> START masterStoryId=X` | Pipeline started (approve or retry) |
| `PIPELINE >>> masterStoryId=X lang=Y DONE Zms` | Language Y finished in Z milliseconds |
| `PIPELINE >>> COMPLETE masterStoryId=X READY` | All languages done; story ready |
| `PIPELINE >>> masterStoryId=X lang=Y TIMEOUT Nmin` | Language Y timed out |
| `PIPELINE >>> masterStoryId=X lang=Y FAILED` | Language Y failed |
| `PIPELINE >>> FAILED masterStoryId=X` | Pipeline failed overall |

## Best Practices

1. **Include IDs** – storyId, parentId, childId, traceId for correlation.
2. **Catch blocks** – Always log and rethrow or handle; no empty catch.
3. **Request lifecycle** – Pipeline triggers (POST/PUT republish, trigger, regenerate) logged at INFO by `RequestLoggingFilter`. High-frequency polling (`/pipeline/active-now`, `/stories/with-issues`) is skipped at DEBUG to reduce log noise.
4. **Production** – Avoid DEBUG; use INFO for business events so aggregators can filter.
