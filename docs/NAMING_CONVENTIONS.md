# Naming Conventions

This document defines naming conventions for the Araro codebase. Apply them consistently in all layers (backend, mobile, web, admin).

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
| Composables | PascalCase | `StoryCard`, `AraroHeroBanner` |
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
