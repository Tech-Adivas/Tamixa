# Naming Conventions Audit

This document records the results of a codebase-wide audit against [NAMING_CONVENTIONS.md](NAMING_CONVENTIONS.md). Use it to keep file names, comments, configuration, and code aligned with project standards.

---

## Summary

| Area | Status | Notes |
|------|--------|--------|
| Backend classes & files | ✅ | Entities, Adapters, Services, Ports, JPA repos, Controllers follow suffixes |
| Backend REST paths | ✅ | kebab-case used consistently |
| Backend API path base | ✅ Fixed | `VoiceCloningController` now uses `ApiVersion.V1` (was hardcoded `/api/v1`) |
| Backend DTO placement | ⚠️ | Some inline DTOs in controllers; see [CODE_REVIEW_REPORT.md](CODE_REVIEW_REPORT.md) |
| Backend config (application.yml) | ✅ | kebab-case for app.*; env vars SCREAMING_SNAKE_CASE |
| Backend env vars (.env.example) | ✅ | SCREAMING_SNAKE_CASE |
| Mobile screens / ViewModels / API | ✅ | *Screen, *ViewModel, *Api file and class names |
| Web pages & components | ✅ | PascalCase file names; kebab-case routes |
| Admin files & routes | ✅ | kebab-case files and route folders; PascalCase component exports |
| Comments / docs | ✅ | Tamixa TTS and tamixa.mp3 used; no stray "sample" references |

---

## 1. Backend (Kotlin)

### 1.1 File and class names

- **Entities:** `*Entity.kt` in `infrastructure/persistence/` — ✅ (e.g. `StoryEntity`, `VoiceProfileEntity`).
- **Ports:** `*Port.kt` / `*RepositoryPort.kt` in `application/port/` — ✅.
- **Adapters:** `*Adapter.kt` in `infrastructure/` — ✅ (e.g. `TamixaTtsClientAdapter`, `StoryRepositoryAdapter`).
- **Services:** `*Service.kt` in `application/` — ✅.
- **JPA repositories:** `*JpaRepository.kt` — ✅.
- **Controllers:** `*Controller.kt` in `api/` — ✅.
- **DTOs:** Under `api/<domain>/dto/` with `*Request`, `*Response`, `*Dto` — ✅ for dedicated DTOs; ⚠️ some controllers still have inline DTOs (see CODE_REVIEW_REPORT).

### 1.2 REST paths

- All `@RequestMapping` / `@GetMapping` etc. use **kebab-case**: e.g. `/curated-stories`, `/stream-url`, `/data-export`, `/voice-cloning`, `/listening-progress`, `/referral-codes`.
- **API version:** Controllers use `${ApiVersion.V1}/...`; **fixed:** `VoiceCloningController` now uses `ApiVersion.V1` instead of hardcoded `/api/v1`.
- **Exception:** `AudioStreamProxyController` maps `/audio` (no `/api/v1` prefix) by design for streaming; documented in CODE_REVIEW_REPORT.

### 1.3 Configuration

- **application.yml:** `app.narration.*` and other custom keys use **kebab-case** (e.g. `tamixa-tts-dir`, `tamixa-tts-default-file`, `google-tts-voice-name`). Hibernate keys (`format_sql`, `default_schema`) use snake_case as per Hibernate convention.
- **Environment variables:** Referenced as `${NARRATION_TAMIXA_TTS_DIR:...}`, `${GOOGLE_CLOUD_TTS_API_KEY:}` — **SCREAMING_SNAKE_CASE** ✅.
- **.env.example:** All variable names SCREAMING_SNAKE_CASE ✅.

### 1.4 Comments and log messages

- Tamixa TTS adapter and related config/docs use **"Tamixa TTS"** and **NARRATION_TAMIXA_TTS_***; no remaining "Sample TTS" or `sample.mp3` / `NARRATION_SAMPLE_*` in code or config.

---

## 2. Mobile (Kotlin)

- **Screens:** `*Screen.kt` (e.g. `LoginScreen`, `DashboardScreen`, `AudioPlayerScreen`) — ✅.
- **ViewModels:** `*ViewModel.kt` (e.g. `AuthViewModel`, `SettingsViewModel`) — ✅.
- **API classes:** `*Api.kt` in `network/` (e.g. `StoryApi`, `AuthApi`, `VoiceApi`) — ✅. `ApiConfig.kt` is config, not an API client; `PlatformApi` is platform interface — acceptable.
- **String keys:** camelCase functions in `Strings` — ✅ per conventions.

---

## 3. Web (React/TypeScript)

- **Pages:** PascalCase file names (`Login.tsx`, `Dashboard.tsx`, `Voice.tsx`) — ✅.
- **Components:** PascalCase (`ErrorBoundary.tsx`, `Skeleton.tsx`, `AppLayout.tsx`) — ✅.
- **Routes:** kebab-case in app routing — ✅.

---

## 4. Admin (Next.js/TypeScript)

- **Files and route folders:** kebab-case — ✅ (e.g. `curated-stories/page.tsx`, `story-for-review/page.tsx`, `voice-logs/`, `ai-metrics/`, `auth-context.tsx`, `use-dashboard.ts`).
- **Routes:** `/dashboard/curated-stories`, `/dashboard/voice-test`, `/dashboard/ai-metrics`, etc. — kebab-case ✅.
- **Component exports:** PascalCase (e.g. `Sidebar`, `VoiceTestPage`) — ✅.
- **Hooks:** `use*` camelCase — ✅.

---

## 5. Cross-cutting

- **Documentation:** All `.md` files under `docs/` or component-specific READMEs; references to Tamixa TTS, `tamixa.mp3`, and `NARRATION_TAMIXA_*` are consistent.
- **User-facing copy:** Per NAMING_CONVENTIONS.md (Default, My voice, My voice & Avatar, etc.) — apply in UI and copy.

---

## 6. Fixes applied in this audit

1. **VoiceCloningController:** `@RequestMapping("/api/v1/voice-cloning")` → `@RequestMapping("${ApiVersion.V1}/voice-cloning")` for consistency with other controllers.

---

## 7. Follow-ups (from CODE_REVIEW_REPORT)

- Move inline DTOs from controllers into `api/<domain>/dto/` where appropriate.
- Consider aligning `AudioStreamProxyController` path with API versioning if product/security requirements change.
- Replace empty `catch (_: Exception) {}` blocks with logging or rethrow in avatar/Replicate and AvatarVideoService.

---

*Last audit: applied after consolidating docs and Tamixa TTS rename. Re-run checks when adding new modules or config.*
