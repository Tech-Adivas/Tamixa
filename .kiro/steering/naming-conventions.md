---
inclusion: always
---

# Naming Conventions

Apply these conventions consistently across the application.

## Backend (Kotlin)

| Element | Convention | Example |
|---------|------------|---------|
| **Classes/Interfaces** | PascalCase | `CuratedStoryService`, `StoryRepositoryPort` |
| **DTOs** | `*Request`, `*Response`, `*Dto` suffix; in `api/<domain>/dto/` | `CreateChildRequest`, `StoryResponse`, `VoiceDto` |
| **Entities** | `*Entity` | `VoiceProfileEntity`, `StoryEntity` |
| **Ports** | `*RepositoryPort`, `*Port` | `CuratedStoryRepositoryPort`, `TtsClientPort` |
| **Adapters** | `*Adapter`, `*RepositoryAdapter` | `GoogleCloudTtsClientAdapter` |
| **Services** | `*Service`, `*ServiceImpl` | `CuratedStoryService`, `EmotionTaggingServiceImpl` |
| **JPA Repositories** | `*JpaRepository` | `CuratedStoryJpaRepository` |
| **REST paths** | kebab-case | `/curated-stories`, `/stream-url` |
| **JSON fields** | camelCase (public API) | `childName`, `audioFileUrl` |
| **Env vars** | SCREAMING_SNAKE_CASE | `API_BASE_URL`, `SENDGRID_API_KEY` |

Use `@JsonProperty("snake_case")` only when integrating with external APIs (OpenAI, etc.); never for internal REST DTOs.

## Mobile (Kotlin)

| Element | Convention | Example |
|---------|------------|---------|
| **Screens** | `*Screen` | `LoginScreen`, `DashboardScreen` |
| **ViewModels** | `*ViewModel` | `AuthViewModel`, `StoryViewModel` |
| **Composables** | PascalCase | `StoryCard`, `TamixaHeroBanner` |
| **API classes** | `*Api` | `StoryApi`, `AuthApi` |
| **DTOs** | `*Response`, `*Dto`; match backend JSON | `CuratedStoryResponse`, `StreamUrlResponse` |
| **String keys** | camelCase functions | `Strings.login()`, `Strings.childName()` |

## Web (React/TypeScript)

| Element | Convention | Example |
|---------|------------|---------|
| **Pages** | PascalCase file name | `Login.tsx`, `Stories.tsx` |
| **Components** | PascalCase | `ErrorBoundary.tsx`, `Skeleton.tsx` |
| **Routes** | kebab-case paths | `/login`, `/auth/magic-link` |
| **API interfaces** | camelCase (match backend) | `accessToken`, `childName` |

## Admin (Next.js/TypeScript)

| Element | Convention | Example |
|---------|------------|---------|
| **Files** | kebab-case (multi-word) | `curated-stories/page.tsx`, `auth-context.tsx` |
| **Route folders** | kebab-case | `ai-metrics/`, `voice-logs/` |
| **Component exports** | PascalCase | `Sidebar`, `RevenueAlertsBanner` |
| **Hooks** | camelCase, `use*` prefix | `useDashboard` |

## DTO Placement

- **Backend**: One DTO per file under `backend/.../api/<domain>/dto/`. No inline DTOs in controllers.
- **Mobile**: Internal DTOs may live in the API module; use `@Serializable` with matching JSON keys.
