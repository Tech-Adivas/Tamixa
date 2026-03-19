# Naming Conventions

This document defines naming conventions for the Tamixa codebase. Apply them consistently in all layers (backend, mobile, web, admin).

---

## Backend (Kotlin)

| Element | Convention | Example |
|--------|-------------|---------|
| Classes / interfaces | PascalCase | `CuratedStoryService`, `StoryRepositoryPort` |
| DTOs | Suffix: `*Request`, `*Response`, `*Dto`; under `api/<domain>/dto/` | `CreateChildRequest`, `StoryResponse`, `VoiceDto` |
| Entities | Suffix: `*Entity` | `VoiceProfileEntity`, `StoryEntity` |
| Ports | Suffix: `*RepositoryPort`, `*Port` | `CuratedStoryRepositoryPort`, `TtsClientPort` |
| Adapters | Suffix: `*Adapter`, `*RepositoryAdapter` | `GoogleCloudTtsClientAdapter` |
| Services | Suffix: `*Service`, `*ServiceImpl` | `CuratedStoryService`, `EmotionTaggingServiceImpl` |
| JPA repositories | Suffix: `*JpaRepository` | `CuratedStoryJpaRepository` |
| REST paths | kebab-case | `/curated-stories`, `/stream-url` |
| JSON fields (public API) | camelCase | `childName`, `audioFileUrl` |
| Environment variables | SCREAMING_SNAKE_CASE | `API_BASE_URL`, `SENDGRID_API_KEY` |

**Note:** Use `@JsonProperty("snake_case")` only for external APIs (e.g. OpenAI); internal REST DTOs use camelCase.

---

## Mobile (Kotlin)

| Element | Convention | Example |
|--------|-------------|---------|
| Screens | Suffix: `*Screen` | `LoginScreen`, `DashboardScreen` |
| ViewModels | Suffix: `*ViewModel` | `AuthViewModel`, `StoryViewModel` |
| Composables | PascalCase | `StoryCard`, `TamixaHeroBanner` |
| API classes | Suffix: `*Api` | `StoryApi`, `AuthApi` |
| DTOs | Suffix: `*Response`, `*Dto`; align with backend JSON | `CuratedStoryResponse`, `StreamUrlResponse` |
| String keys | camelCase functions | `Strings.login()`, `Strings.childName()` |

---

## Web (React / TypeScript)

| Element | Convention | Example |
|--------|-------------|---------|
| Pages | PascalCase file name | `Login.tsx`, `Stories.tsx` |
| Components | PascalCase | `ErrorBoundary.tsx`, `Skeleton.tsx` |
| Routes | kebab-case paths | `/login`, `/auth/magic-link` |
| API / types | camelCase (match backend) | `accessToken`, `childName` |

---

## Admin (Next.js / TypeScript)

| Element | Convention | Example |
|--------|-------------|---------|
| Files (multi-word) | kebab-case | `curated-stories/page.tsx`, `auth-context.tsx` |
| Route folders | kebab-case | `ai-metrics/`, `voice-logs/` |
| Component exports | PascalCase | `Sidebar`, `RevenueAlertsBanner` |
| Hooks | camelCase, `use*` prefix | `useDashboard` |

---

## DTO placement

- **Backend:** One DTO per file under `backend/.../api/<domain>/dto/`. No inline DTOs in controllers.
- **Mobile:** DTOs may live in the API module; use `@Serializable` with keys matching backend JSON.

---

## User-facing copy (Voice & Avatar)

Use these consistently in the app (story screen, settings, My voice & Avatar tab):

| Concept | Label (EN) | Notes |
|--------|------------|--------|
| System / default narration | **Default** | No user voice |
| User’s own voice (family or cloned profile) | **My voice** | One label for both; aligns with “My voice & Avatar” tab. Do not show “Cloned Voice” in UI. |
| Premium / paid voice | **&lt;Name&gt; ★** | e.g. “Calm ★” |
| Tab for voice + avatar | **My voice & Avatar** | Single bottom tab; hub to clone voice and add avatar. |
| In-player: record for this story | **Record voice for this story** | Per-story recording option. |
| After clone success (CTA) | **Add your photo — bring stories to life** | Shown when user has credits. |

---

## UI color consistency (mobile)

Use theme-based colors so text is visible in all themes and on all screens:

| Where | Use | Avoid |
|-------|-----|--------|
| **Card container + content** | `TamixaCardColors.surface()`, `.primaryContainer()`, `.errorContainer()`, etc. (sets both container and content color) | `CardDefaults.cardColors(containerColor = ...)` without `contentColor` |
| **Text inside cards** | `TamixaContentColors.cardPrimary()`, `.cardSecondary()`, `.onPrimaryContainer()`, etc. | `TamixaColors.cream` / `lavenderGlow` inside cards (those are for text on dark screen backgrounds only) |
| **Screen headlines** (on dark bg) | `TamixaColors.cream`, `TamixaColors.lavenderGlow` | — |
