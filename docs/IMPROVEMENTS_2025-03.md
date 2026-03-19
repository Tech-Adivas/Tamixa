# App Improvements (March 2025)

Architecture review as solution architect + coding + AI expert. Implemented high-impact changes across security, reliability, and developer experience.

## Summary of Changes

### Security (P0)

| Item | Status | Notes |
|------|--------|------|
| JWT secret validation in prod | ✅ Already present | `JwtSecretValidator` fails startup if `JWT_SECRET` is default when `prod` profile active |

### Reliability (P1)

| Item | Change |
|------|--------|
| **OpenAI completeChat** | Now throws `OpenAIException` on API failure instead of returning `""`. Callers: `ShortContentGenerationService` (catches, returns empty), `ConversationSummarizer` (catches, returns null), `LibraryStoryRephraseService` (propagates → 502) |
| **OpenAIException** | Added optional `cause: Throwable?` to preserve stack trace |
| **Avatar/Replicate catches** | HeyGen + ReplicateSadTalker: replaced silent `catch (_: Exception)` with `log.debug` when parsing error response bodies fails |

### Developer Experience (P3)

| Item | Change |
|------|--------|
| **Admin API base URL** | Exported `getApiBaseUrl()` from `admin/src/lib/api.ts`. Stories pages now use it instead of inline `"http://localhost:8080"` for cover image resolution |
| **Cover URL resolution** | Uses `NEXT_PUBLIC_API_URL` when set; preserves SSR behavior (relative paths when base empty) |

## Files Touched

- `backend/.../OpenAIClient.kt` – completeChat throws, OpenAIException supports cause
- `backend/.../ShortContentGenerationService.kt` – try/catch completeChat
- `backend/.../ConversationSummarizer.kt` – try/catch completeChat
- `backend/.../HeyGenAvatarVideoClient.kt` – debug logging in error-body parse catches
- `backend/.../ReplicateSadTalkerClient.kt` – debug logging in error-body parse catch
- `admin/src/lib/api.ts` – export `getApiBaseUrl`
- `admin/.../stories/page.tsx` – use `getApiBaseUrl` for cover
- `admin/.../stories/[id]/edit/page.tsx` – use `getApiBaseUrl` for cover

## Next Steps Implemented (March 2025)

### 1. Pagination for library list
- **Backend**: `LibraryStoryController.list` returns `PagedResponse<LibraryStoryResponse>` with content, page, size, totalElements, totalPages, first, last
- **Mobile**: Added `LibraryStoriesPageResponse`, `StoryApi.getLibraryStories` parses paged response and returns content; supports optional `theme` filter
- **Test**: `LibraryStoryControllerTest` updated for paged shape

### 2. S3 bucket centralization
- **AppProperties.StorageProperties**: Added `effectiveS3Bucket` getter (s3Bucket or default "tamixa-audio")
- Replaced all `appProperties.storage.s3Bucket.ifBlank { "tamixa-audio" }` usages across 14 files with `appProperties.storage.effectiveS3Bucket`

### 3. Redis-backed rate limiting
- **Config**: `app.rate-limit.use-redis` (RATE_LIMIT_USE_REDIS), default false
- **RedisRateLimitingFilter**: Fixed-window counter using Redis INCR + EXPIRE; enabled when use-redis=true and Redis available
- **RateLimitingFilter**: Loads only when use-redis=false (matchIfMissing=true)
- **Fail-open**: On Redis failure, allows request and logs warning

### 4. Admin controller split
- Deferred (large refactor)

## Recommended Future Work

1. **Admin controller split** – Break up `AdminController` by domain (stories, parents, moderation, etc.)
2. **Library load-more** – Mobile UI to fetch page > 0 when user scrolls
