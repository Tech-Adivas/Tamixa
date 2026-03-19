# Tamixa — Gap Analysis & Improvements

**Date:** February 2026 (updated March 2026)  
**Scope:** Full application analysis and targeted improvements.

---

## Deep Analysis Summary

| Layer | Finding | Severity | Status |
|-------|---------|----------|--------|
| **Backend** | CORS `allowedOriginPatterns: ["*"]` with `allowCredentials: true` — security risk in prod | High | ✅ CorsOriginValidator fails prod startup if CORS_ALLOWED_ORIGINS unset or `*` |
| **Backend** | `org.hibernate.SQL: DEBUG` in base config; prod should override to WARN | Medium | ✅ application-prod.yml |
| **Backend** | Rate limiter doesn't expose `X-RateLimit-*` headers — clients can't show quota | Low | ✅ Exposed |
| **Backend** | Silent catch blocks (ReplicateSadTalker, WebhookEncryptor, S3FamilyVoice) | High | ✅ Fixed Mar 2026 |
| **Backend** | Auth DTOs inline in controller | Medium | ✅ Moved to api/auth/dto/ |
| **Backend** | VoiceCloningController `/tiers` no @PreAuthorize | Low | ✅ Added PARENT |
| **Backend** | getAvailableVoices masks errors with empty list | Medium | ✅ Logging improved |
| **Mobile** | Parent's generated stories not fetched from server — only cached + curated | High | ✅ Fixed |
| **Web** | No Error Boundary — unhandled React errors crash full app | Medium | ✅ Fixed |
| **Web** | No child management page | Medium | ✅ Added |
| **Web** | Register form lacks client-side password validation feedback | Low | ✅ Fixed |

---

## 1. Improvements Implemented

### 1.1 Backend: Parent-Facing Story List API

**Gap:** Parents had no way to list their AI-generated stories. The API only exposed `POST /stories/generate`; the admin `GET /admin/stories` was admin-only.

**Fix:** Added `GET /api/v1/stories` for parents:

- `StoryRepositoryPort.findByParentId(parentId, pageable)`
- `StoryJpaRepository.findByParent_Id(parentId, pageable)`
- `StoryService.findByParent(parentEmail, page, size)`
- `StoryController.list(page, size)` → `StoriesPageResponse`

### 1.2 Web Frontend: Full API Integration

**Gap:** The web app had placeholder screens (Login, Stories) with no API integration.

**Fix:** Implemented:

- **API client** (`web/src/lib/api.ts`): Auth (login, register, refresh, getMe), children, curated stories, generated stories, stream URL, token storage
- **Auth context** (`web/src/contexts/AuthContext.tsx`): Session state, logout, protected routes
- **Pages:**
  - `Home` — Landing with login/register links
  - `Login` / `Register` — Functional forms with API calls
  - `Dashboard` — Welcome and links to stories
  - `Stories` — Browse curated library, view generated stories, generate new AI stories
- **Protected routes** — Redirect unauthenticated users to `/login`
- **Styles** — Form, buttons, tabs, story cards

### 1.3 Admin API: Non-OK Response Handling

**Gap:** Admin API methods called `.then(r => r.json())` without checking `res.ok`, so 4xx/5xx responses could be treated as success.

**Fix:** Introduced `fetchJson<T>()` that throws on non-OK responses and parses error messages from the body. All read endpoints now use `fetchJson`. Mutations (`flagStory`, `createCuratedStory`) perform explicit `res.ok` checks and throw with error messages.

### 1.4 Admin Kafka Page: Pagination Logic

**Gap:** Previous button used `disabled={!data?.first}`, which disables when *not* on first page. Previous should be disabled when *on* first page.

**Fix:** Changed to `disabled={data?.first ?? true}` so Previous is disabled when `data.first` is true.

### 1.5 Web: Environment Example

**Gap:** No `.env.example` for the web module.

**Fix:** Added `web/.env.example` with a commented `VITE_API_URL` for production builds.

### 1.6 Mobile: Parent's Generated Stories from Server

**Gap:** Mobile only showed cached (in-session) + curated stories. Past AI-generated stories were never fetched from server.

**Fix:** Added `StoryApi.getMyStories()`, `StoryRepository.getMyStories()`, `StoryViewModel.loadMyStories()` and `myStories` state. Dashboard and StorySelection now load and display parent's generated stories. `allStories()` merges server-generated + cached + curated with deduplication.

### 1.7 Web: Error Boundary

**Gap:** Unhandled React errors caused full app crash with no user feedback.

**Fix:** Added `ErrorBoundary` component wrapping the app; shows fallback UI with error details and link to home.

### 1.8 Backend: Production Hardening

**Gap:** SQL debug logging and permissive CORS in production.

**Fix:**
- `application-prod.yml`: Set `org.hibernate.SQL: WARN` to reduce log volume.
- `AppProperties.cors`: Added configurable `allowedOrigins` (comma-separated). Set `CORS_ALLOWED_ORIGINS` in prod for explicit origins.
- Exposed `X-RateLimit-Limit` header on successful rate-limited requests.

### 1.9 Web: Children Page & Form Validation

**Gap:** No child profile management; weak form validation.

**Fix:**
- Added `/children` page: list children, add child (name, DOB, language).
- Register: client-side password length validation (min 8 chars) with immediate feedback.
- Stories generate form: required-field validation before submit.

---

## 1.10 Improvements (March 2026 – Senior Tech Lead Audit)

**Gap:** Silent catch blocks, inline DTOs, and minor security/documentation gaps identified in full codebase audit.

**Fix:**
- **ReplicateSadTalkerClient**: Removed debug file writes and silent catches; replaced with structured `log.warn`/`log.error`.
- **WebhookPayloadEncryptor**: Added `log.warn` when key decode/size check fails (no silent return).
- **S3FamilyVoiceStorageAdapter**: Added `log.debug` in `exists()` catch (object-not-found is expected; logging aids debugging).
- **VoiceCloningController**: Added `@PreAuthorize("hasRole('PARENT')")` to `GET /tiers` for consistency.
- **StreamUrlController**: Improved `getAvailableVoices` error log with exception details; still returns empty list for resilience.
- **Auth DTOs**: Moved `OtpSendRequest`, `OtpSendResponse`, `OtpVerifyRequest`, `PasswordlessRequest`, `PasswordlessVerifyRequest`, `PasswordlessSendResponse`, `CurrentUserResponse` to `api/auth/dto/`; added `@NotBlank` validation.
- **.env.example**: Documented `CORS_ALLOWED_ORIGINS` for prod.

---

## 2. Remaining Gaps & Recommendations

### 2.1 Web Frontend

| Gap | Recommendation |
|-----|----------------|
| No audio playback in browser | Add HTML5 audio element with `getStreamUrl`; show play/pause for stories with `audioFileUrl` |
| No child profile management | Add Children page: list, add, edit (dates, interests) |
| No error boundary | Add React Error Boundary for unhandled errors |
| No loading skeletons | Replace "Loading…" with skeleton components for better UX |
| No password reset | Backend may need reset flow; web would need reset page |

### 2.2 Mobile

| Gap | Recommendation |
|-----|----------------|
| iOS not implemented | `iosMain` exists but is largely stubs; complete iOS targets |
| Generated stories not persisted in app | Consider local cache of generated story IDs for offline "My stories" |

### 2.3 Backend

| Gap | Recommendation |
|-----|----------------|
| Passwordless login (ROADMAP Phase 1) | OTP or magic links per COMPLIANCE and roadmap |
| No rate limit headers | Consider `X-RateLimit-*` headers for client visibility |
| Consent recording | COMPLIANCE requires explicit consent at registration and per child; ensure workflow is implemented and auditable |

### 2.4 Admin Dashboard

| Gap | Recommendation |
|-----|----------------|
| Kafka / Audit pages are stubs | Backend returns empty pages; integrate with log aggregator or add audit/Kafka tables if needed |
| No story content preview | Admin moderation could show story text inline |

### 2.5 Infrastructure

| Gap | Recommendation |
|-----|----------------|
| CloudFormation empty | Populate `infra/cloudformation/` with AWS templates if using AWS |
| No CI/CD config | Add GitHub Actions / GitLab CI for test, lint, build |

### 2.6 Testing

| Gap | Recommendation |
|-----|----------------|
| Web has no tests | Add Vitest + React Testing Library for critical flows |
| Admin has no tests | Add Jest/Playwright for key admin flows |

---

## 3. Architecture Summary

| Component | Stack | Status |
|-----------|-------|--------|
| Backend | Spring Boot 3, Kotlin, PostgreSQL, Redis, Kafka | Feature-complete; hexagon architecture |
| Mobile | KMP, Compose, Ktor | Android complete; iOS-ready |
| Web | React, Vite, TypeScript | Functional; auth + stories integrated |
| Admin | Next.js, Radix, Tailwind | Functional; API error handling fixed |

---

## 4. Compliance Notes

Per `docs/COMPLIANCE.md`:

- **DPDP, COPPA, GDPR** — Parental consent, minimal collection, encryption, audit logging.
- **Implementation checklist** — Review `docs/COMPLIANCE.md` Section 16 for engineer tasks.
