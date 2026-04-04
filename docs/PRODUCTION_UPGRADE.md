# Production Readiness Upgrade Summary

This document summarizes the production hardening changes applied to the backend.

## 1. Security Hardening

- **Rate limiting**: `RateLimitingFilter` (Bucket4j) already present; response format aligned with `ErrorResponse` (traceId, timestamp).
- **Request size**: File upload limit remains 10MB in `application.yml`; voice upload validates against `app.voice.max-file-size-bytes`.
- **No raw voice storage**: Confirmed—only encrypted embedding is persisted; temp file is deleted in `VoiceService`. Comment added in code.
- **AES-256**: `AesEncryptionService` (AES/GCM) already in place for voice embeddings.
- **Audit logging**: New `AuditLogPort` and `StructuredAuditLogger` for:
  - Login attempts (success/failure)
  - Story generation
  - Voice upload
  - Subscription changes (ready for future use)
- **Global exception handler**: `ErrorResponse` now includes `traceId` and `timestamp`; all handlers use consistent format.
- **JWT secret**: `JwtSecretValidator` fails startup in **prod** if default dev secret is still used. Set `JWT_SECRET` in production.
- **CORS**: `CorsConfigurationSource` added in `SecurityConfig` (allowCredentials, methods, headers, exposedHeaders). Restrict origins in prod via config if needed.

## 2. Observability

- **Actuator**: Already enabled (health, info, metrics, prometheus). Health probes enabled for liveness/readiness.
- **Structured JSON logging**: Already in place via `logback-spring.xml` for prod/staging.
- **Correlation ID**: `RequestTracingFilter` sets MDC `traceId` and `X-Request-Id`; `ErrorResponse` includes traceId.
- **Custom metrics** (`ApplicationMetrics`):
  - `story.generation.latency` (timer)
  - `voice.processing.latency` (timer)
  - `story.cache.hits` / `story.cache.misses` (counters)
- **Kafka consumer lag**: Available via Micrometer when Kafka is active; ensure `management.metrics` includes Kafka in prod.

## 3. Performance & Scalability

- **HikariCP**: Base tuning in `application.yml` (connection-timeout, idle-timeout, max-lifetime, keepalive-time). Profile-specific pool sizes in dev (5) and prod (20).
- **Transactions**: `@Transactional` added on `AuthService.register`, `StoryService.generate`, `VoiceService.uploadVoice`.
- **OpenAI**: Timeouts configurable via `app.openai.connect-timeout-ms` and `read-timeout-ms`; `RestTemplate` uses these. **Retry** with exponential backoff via `@Retryable` on `OpenAIClient` (maxAttempts=3, backoff multiplier 2).

## 4. Async / Kafka

- **Story status**: `StoryStatus` extended with `PROCESSING` and `FAILED`. Flow: PENDING → PROCESSING → READY | FAILED.
- **Repository**: `StoryRepositoryPort.updateStatus(id, status)` added; listener sets PROCESSING before work, READY on success, FAILED on exception (and in DLT recoverer).
- **DLT**: Dead-letter handling in `DefaultErrorHandler`—on final failure, story is set to FAILED and the record is sent to `app.kafka.topics.story-created-dlt` (default: `story-created.DLT`).
- **Retry**: `ExponentialBackOff` for the audio consumer (configurable interval and multiplier).
- **Idempotency**: Listener skips if story is already READY; otherwise sets PROCESSING and proceeds.

## 5. Configuration

- **application-staging.yml**: Added for staging profile (DB, pool size, logging).
- **@ConfigurationProperties**: `AppProperties` added under `app`; used in `RestTemplateConfig` and `JwtSecretValidator`. Other beans can be migrated from `@Value` to `AppProperties` over time.
- **No hardcoded values**: Timeouts, limits, and topic names come from `application.yml` and env vars.

## 6. Testing

- **SecurityIntegrationTest**: Verifies 401 for unauthenticated access to `/children` and `/voice`, and that actuator health is public.
- **StoryControllerTest**: `@WebMvcTest` for `StoryController`—generate story with `@WithMockUser` and unauthenticated returns 401.
- Existing **AuthApiIntegrationTest** and **IntegrationTestBase** (Postgres Testcontainers) unchanged; new beans (e.g. `AuditLogPort`, `ApplicationMetrics`) are real in test context.

## 7. Cleanup

- Package boundaries and SOLID preserved; new components follow existing ports/adapters.
- Imports and style kept consistent; no duplicate logic introduced.

---

## Required Production Environment Variables

- `JWT_SECRET` (required in prod; startup fails if default is used)
- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- LLM: `AI_LLM_PROVIDER` (`openai` or `gemini`) and matching key — `OPENAI_API_KEY` when OpenAI; `GEMINI_API_KEY` when Gemini (and for Gemini covers / Veo when enabled)
- Still cover image: `AI_COVER_IMAGE_PROVIDER` (`openai` = DALL·E + `OPENAI_API_KEY`; `gemini` = Gemini image + `GEMINI_API_KEY`)
- `VOICE_ENCRYPTION_KEY` (for voice uploads)
- Optional: `CORS_ALLOWED_ORIGINS` (if you add origin restriction in `SecurityConfig`)

## Optional: Kafka Topics

Create topics if not auto-created, e.g.:

```bash
kafka-topics --create --topic story-created --bootstrap-server <broker>
kafka-topics --create --topic story-created.DLT --bootstrap-server <broker>
kafka-topics --create --topic curated-story-created --bootstrap-server <broker>
```

## Curated Stories (Admin Upload)

- Admins upload story **text only** via `/dashboard/curated-stories`; audio is generated automatically.
- Kafka topic `curated-story-created` triggers TTS pipeline (same as AI stories).
- Set `KAFKA_TOPIC_CURATED_STORY_CREATED` in production (default: `curated-story-created`).

## Running Tests

If using Gradle wrapper:

```bash
./gradlew test
```

Otherwise ensure `JWT_SECRET` and other env vars are set for integration tests (or rely on test profile defaults).
