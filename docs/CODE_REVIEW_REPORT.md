# Tamixa Kids – Full Code Review Report

**Date:** March 4, 2026  
**Scope:** Backend (Kotlin), Mobile (KMP/Compose), Web (React), Admin (Next.js)

---

## 1. NAMING CONVENTIONS

### ✅ Good Practices Found

- **Backend DTOs:** Consistent `*Request`, `*Response`, `*Dto` suffixes in dedicated files under `api/<domain>/dto/`
- **Entities:** `*Entity` naming (e.g. `StoryEntity`, `VoiceProfileEntity`)
- **Ports/Adapters:** `*Port`, `*Adapter` pattern in application and infrastructure layers
- **REST paths:** kebab-case (`/curated-stories`, `/stream-url`, `/data-export`)
- **JSON fields:** camelCase in internal REST DTOs; `@JsonProperty("snake_case")` only for external APIs (OpenAI, etc.)
- **Env vars:** SCREAMING_SNAKE_CASE in `.env.example` (e.g. `API_BASE_URL`, `SENDGRID_API_KEY`)
- **Mobile:** Screens use `*Screen` suffix; ViewModels use `*ViewModel`; API classes use `*Api`; Strings use camelCase functions (`Strings.login()`, `Strings.childName()`)
- **Web:** PascalCase pages (`Login.tsx`, `Dashboard.tsx`); PascalCase components (`ErrorBoundary`, `Skeleton`)
- **Admin:** kebab-case files (`curated-stories/page.tsx`, `auth-context.tsx`); PascalCase component exports

### ⚠️ Issues

| Issue | Location | Severity |
|-------|----------|----------|
| **Inline DTOs in controllers** – Several controllers define DTOs inline instead of in dedicated files per naming conventions | `StoryController` (StoriesPageResponse, SearchStoryItem, SearchStoriesResponse), `AuthController` (CurrentUserResponse, OtpSendRequest, OtpSendResponse, OtpVerifyRequest), `DataExportController` (ExportJobResponse), `FavoriteStoryController` (FavoriteStoryResponse, FavoriteCheckResponse), `FeedbackController` (FeedbackRequest), `SoundscapeController` (SoundscapeDto), `ListeningProgressController` (ListeningProgressResponse), `StreamAnalyticsController` (StreamAnalyticsRequest) | **Medium** |
| **Inconsistent API path base** – `AudioStreamProxyController` uses `/audio` without `/api/v1` prefix; `CoverImageProxyController` uses `${ApiVersion.V1}/covers` | `AudioStreamProxyController.kt:21`, `CoverImageProxyController.kt:25` | **Low** |
| **Duplicate Screen/ViewModel modules** – Some screens exist in both `composeApp` and `shared` (e.g. `StoryGenerationScreen`, `DashboardScreen`, `LoginScreen`) | `mobile/composeApp/` vs `mobile/shared/` | **Low** |

---

## 2. SECURITY

### ✅ Good Practices Found

- **`.env` is gitignored** – `.gitignore` includes `.env`, `.env.local`, `*.env`
- **@PreAuthorize** – All protected controllers use `@PreAuthorize("hasRole('PARENT')")` or `@PreAuthorize("hasRole('ADMIN')")`
- **No raw SQL** – No `createNativeQuery`, string-concatenated SQL, or `@Query` with `$` interpolation
- **PII masking** – AuthController, OtpService, LoggingEmailSender, StructuredAuditLogger use `PiiMask.maskEmail()` / `PiiMask.maskPhone()` for sensitive data in logs
- **Dev endpoints** – DevSeedController, DevVerifyConnectionsController behind `@Profile("dev")`; SEED_ADMIN_ENABLED guard
- **Webhook signature verification** – WebhookController verifies Stripe/Zoho signatures
- **No empty catch blocks in application code** – Backend Kotlin catch blocks log and/or rethrow; mobile composeApp StoryApi logs failures

### ⚠️ Issues

| Issue | Location | Severity |
|-------|----------|----------|
| **PII in logs** – SendGridEmailSender logs raw email addresses | `SendGridEmailSender.kt:52,55` – `log.info("Magic link email sent to $toEmail")`, `log.warn("Failed to send magic link email to $toEmail...")` | **High** |
| **PII in debug logs** – JwtAuthenticationFilter logs user email in debug | `JwtAuthenticationFilter.kt:38` – `log.debug("Authenticated user: {}", claims.email)` | **Medium** |
| **Missing @Valid on request bodies** – AdminController `suggestRephrase` accepts `Map<String, String?>` with no validation; optional `RepublishRequest?` has no validation when provided | `AdminController.kt:226`, `AdminController.kt:458` | **Medium** |
| **Unvalidated Map body** – `suggestRephrase` accepts arbitrary JSON as Map; no size limits or allowlist for keys/values | `AdminController.kt:224-229` | **Medium** |
| **Silent exception swallowing in mobile shared StoryApi** – Catches return `null`/`false`/`0` without logging | `mobile/shared/.../StoryApi.kt:34,54,68,109` – `catch (_: Exception) { null }` | **Medium** |
| **SubscriptionController wrong status for unauthenticated** – Uses `notFound()` when auth name is null; 401 is more appropriate if this path is reachable | `SubscriptionController.kt:56-57,98-99` | **Low** |
| **DevVerifyConnectionsController logs key presence** – `keyConfigured={}` is safe (boolean); no secret leakage | N/A – acceptable | - |

---

## 3. PROFESSIONAL STANDARDS

### ✅ Good Practices Found

- **Structured logging** – traceId, storyId, parentId used for traceability; appropriate info/warn/error levels
- **Error handling** – GlobalExceptionHandler maps domain exceptions to correct HTTP status; catches log and rethrow
- **Thin controllers** – Controllers delegate to services; most business logic in application layer
- **Defensive validation** – `@Valid` on most POST/PUT bodies; allowlists for theme/language where applicable
- **No stack traces in production** – `isDev` check in GlobalExceptionHandler; user-facing messages sanitized

### ⚠️ Issues

| Issue | Location | Severity |
|-------|----------|----------|
| **JwtAuthenticationFilter catch does not rethrow** – Swallows JWT validation failure; continues filter chain. Intentional (anonymous access) but could log at DEBUG for troubleshooting | `JwtAuthenticationFilter.kt:41-43` | **Low** |
| **StreamUrlController catch uses LoggerFactory inline** – `org.slf4j.LoggerFactory.getLogger(...)` called inside catch; should use class-level logger | `StreamUrlController.kt:153-155` | **Low** |
| **AdminController trigger-pipeline sync path** – Background thread catch logs but does not propagate; by design for fire-and-forget, acceptable | `AdminController.kt:411-416` | **Low** |
| **FamilyAvatarController/StreamUrlController catch blocks** – Some return ResponseEntity without body for NotFound (e.g. `ResponseEntity.notFound().build()`) – valid for 404 | N/A – acceptable | - |

---

## 4. ARCHITECTURE

### ✅ Good Practices Found

- **Hexagonal pattern** – Services inject ports; adapters implement ports; Spring wires implementations
- **DTO placement** – Many DTOs in dedicated files under `api/<domain>/dto/` (49 DTO files)
- **Mobile commonMain separation** – Shared UI, network, domain in commonMain; platform-specific (TokenStorage, PlatformTime, ExoPlayer) in androidMain/iosMain

### ⚠️ Issues

| Issue | Location | Severity |
|-------|----------|----------|
| **Direct S3Client injection in controllers** – AdminController, AudioStreamProxyController, CoverImageProxyController, DevVerifyConnectionsController inject `S3Client` directly instead of a storage port | `AdminController.kt`, `AudioStreamProxyController.kt`, `CoverImageProxyController.kt`, `DevVerifyConnectionsController.kt` | **Medium** |
| **Inline DTOs** – See Naming Conventions section | Multiple controllers | **Medium** |

---

## 5. API DESIGN

### ✅ Good Practices Found

- **ErrorResponse** – Consistent shape: `message`, `status`, `traceId`, `timestamp`; used by GlobalExceptionHandler
- **PagedResponse** – Admin pagination uses `PagedResponse<T>` with `content`, `totalElements`, `totalPages`, etc.
- **HTTP status usage** – 201 Created for create (ChildController, FamilyAvatarController, StreamUrlController); 401 for auth failures; 403 for access denied; 404 for not found; 422 for moderation failures; 429 for rate limit
- **Request/Response naming** – `*Request` and `*Response` used consistently for DTOs

### ⚠️ Issues

| Issue | Location | Severity |
|-------|----------|----------|
| **SubscriptionController getUsage/getCurrent** – Return 404 when `authentication?.name` is null or parent not found; with @PreAuthorize, unauthenticated users get 403 before reaching controller. If auth context is partially set, 401 would be more correct than 404 | `SubscriptionController.kt:56-57,98-99` | **Low** |
| **DataExportController request()** – Returns 200 OK for export request; 202 Accepted may be more accurate for async job creation | `DataExportController.kt:25-29` | **Low** |
| **CoverImageProxyController / AudioStreamProxyController** – Logs show `ErrorResponse` with `image/png` Content-Type conflict when cover fails; error handling may need to distinguish JSON vs binary responses | `backend/logs/` (HttpMessageNotWritableException) | **Medium** |

---

## 6. TESTING

### ✅ Good Practices Found

- **Auth tests** – AuthServiceTest, AuthApiIntegrationTest, AuthConsentIntegrationTest
- **Story tests** – StoryControllerTest, StoryProcessingOrchestratorIntegrationTest, StoryProcessingServiceIdempotencyTest
- **Narration tests** – TTSServiceTest, NarrationFormatterServiceTest, EmotionTaggingServiceImplTest, SSMLBuilderServiceImplTest, SSMLEmotionMappingTest, RewriteValidationTest, RetryStrategyTest
- **Security tests** – SecurityIntegrationTest, RateLimitingIntegrationTest
- **Integration test base** – IntegrationTestBase, TestFamilyVoiceStorageConfig for Testcontainers-style setup

### ⚠️ Issues

| Issue | Location | Severity |
|-------|----------|----------|
| **No WebhookController tests** – Stripe/Zoho webhook handling and signature verification not covered | - | **High** |
| **No SubscriptionController payment flow tests** – Checkout session creation, upgrade, cancel not tested | - | **High** |
| **No end-to-end story generation tests** – Full pipeline from generate → narration → stream not covered | - | **Medium** |
| **Test naming** – Mix of `*Test` and `*IntegrationTest`; consistent but some domain tests could be more descriptive | - | **Low** |

---

## 7. DOCUMENTATION

### ✅ Good Practices Found

- **AGENTS.md** – AI/developer guidelines with priorities and code review checklist
- **[SECURITY.md](SECURITY.md)** – Security controls, validation, auth, CORS, rate limiting
- **[backend/SECURITY_CHECKLIST.md](backend/SECURITY_CHECKLIST.md)** – Pre-release verification
- **[mobile/OFFLINE_CACHE.md](mobile/OFFLINE_CACHE.md)** – Offline cache behavior
- **Doc comments** – ApiVersion, WebhookController, ApiErrorExtractor, AudioStreamProxyController, GlobalExceptionHandler have useful comments

### ⚠️ Issues

| Issue | Location | Severity |
|-------|----------|----------|
| **Missing doc comments on public API DTOs** – Most DTOs have no KDoc; fields like `successUrl`/`cancelUrl` in UpgradeRequest could use constraints explanation | Various dto/*.kt | **Low** |
| **README accuracy** – Should be verified against current module structure and env vars | README.md | **Low** |

---

## Summary: Priority Actions

| Priority | Action | Status |
|---------|--------|--------|
| **High** | Fix PII logging in SendGridEmailSender (use PiiMask.maskEmail) | ✅ **Fixed** |
| **High** | Add WebhookController and SubscriptionController payment tests | Pending |
| **Medium** | Add @Valid + DTO for suggestRephrase | ✅ **Fixed** |
| **Medium** | Mask email in JwtAuthenticationFilter debug log | ✅ **Fixed** |
| **Medium** | Add logging to mobile shared StoryApi catches | ✅ **Fixed** |
| **Medium** | Use class-level logger in StreamUrlController catch | ✅ **Fixed** |
| **Medium** | Validate RepublishRequest when provided; Refactor S3Client to storage port; Fix CoverImageProxy/AudioStreamProxy error response Content-Type | Pending |
| **Low** | Move inline DTOs to dedicated files; Fix SubscriptionController 404→401; Add KDoc to key DTOs; Unify AudioStreamProxyController path prefix | Pending |

---

## Fixes Applied (March 4, 2026)

- **SendGridEmailSender**: Replaced raw email in log messages with `PiiMask.maskEmail(toEmail)`.
- **JwtAuthenticationFilter**: Replaced raw email in debug log with `PiiMask.maskEmail(claims.email)`.
- **StreamUrlController**: Added class-level `log` and replaced inline `LoggerFactory.getLogger()` in catch block.
- **AdminController.suggestRephrase**: Replaced unvalidated `Map<String, String?>` with validated `SuggestRephraseRequest` DTO (`@Size(max=255)` for title, `@Size(max=50_000)` for content).
- **mobile/shared/StoryApi**: Added logging in all catch blocks (`println` with operation context) for getCuratedStoryById, isFavorite, getStreamUrl, getPlaybackPosition, reportStreamAnalytics.

---

*Report generated from codebase exploration and pattern analysis. Fixes applied per code review.*
