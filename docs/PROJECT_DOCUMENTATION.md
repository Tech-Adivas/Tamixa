# Tamixa (araro-kids) — Project documentation

This document is the **single entry point** for understanding the Tamixa monorepo: what it is, who uses it, how pieces connect, and where to read more. It uses **Mermaid** diagrams (render in GitHub, GitLab, many IDEs, and Markdown preview tools).

**Related deep dives:** [ARCHITECTURE.md](ARCHITECTURE.md) (integrations map), [ARCHITECTURE_AND_FLOWS.md](ARCHITECTURE_AND_FLOWS.md) (mobile routes ↔ API), [backend/MODULAR_MONOLITH_STRUCTURE.md](backend/MODULAR_MONOLITH_STRUCTURE.md), [GETTING_STARTED.md](GETTING_STARTED.md). **Agentic SDLC:** [AGENTS.md](../AGENTS.md), [.github/prompts/](../.github/prompts/), [AGENTIC_SDLC_GOVERNANCE.md](AGENTIC_SDLC_GOVERNANCE.md), [MCP_POLICY.md](MCP_POLICY.md), [AI_EVALUATION_SYSTEM.md](AI_EVALUATION_SYSTEM.md), [AI_GOVERNANCE_E2E.md](AI_GOVERNANCE_E2E.md), [COST_GOVERNANCE.md](COST_GOVERNANCE.md), [CONTEXT_LIFECYCLE.md](CONTEXT_LIFECYCLE.md), [AGENT_EXECUTION_BOUNDARIES.md](AGENT_EXECUTION_BOUNDARIES.md), [TAMIXA_AI_CONTROL_PLANE.md](TAMIXA_AI_CONTROL_PLANE.md), [CONTROL_PLANE_ARCHITECTURE.md](CONTROL_PLANE_ARCHITECTURE.md), [METRICS_EXPORT.md](METRICS_EXPORT.md), [engineering-brain/README.md](engineering-brain/README.md), [context-packs/README.md](context-packs/README.md), [adr/README.md](adr/README.md), [specs/README.md](specs/README.md).

---

## 1. Product overview

**Tamixa** is an AI-assisted storytelling product for families: parents configure child-friendly profiles, browse a **story library**, generate **personalized stories**, listen with **TTS** (and optional **cloned parent voice**), and optionally use **avatar / video** experiences. An **admin** web app supports content operations, moderation, and pipeline tooling. A **parent web** client (React) complements the primary **mobile** app (Kotlin Multiplatform + Compose).

| Stakeholder | Primary surface | Goals |
|-------------|-----------------|--------|
| Parent / caregiver | Mobile (first), Web | Safe stories, personalization, subscription, voice/avatar setup |
| Child (via parent device) | Mobile | Listening, simple navigation within parent-controlled flows |
| Operations / content | Admin (Next.js) | Curated stories, approvals, users, health, revenue views |
| Platform / integrations | Backend | Auth, billing webhooks, AI/TTS vendors, storage, push |

---

## 2. Monorepo layout

Gradle includes **`backend`**, **`web`**, and **`mobile`** (see root `settings.gradle.kts`). **`admin`** is a sibling Node/Next.js app under `admin/` (not always wired as a Gradle submodule; run via `npm`).

```mermaid
flowchart TB
  subgraph repo [tamixa / araro-kids monorepo]
    M[mobile — KMP Compose]
    B[backend — Spring Boot Kotlin]
    W[web — React Vite TS]
    A[admin — Next.js]
  end

  M -->|HTTPS REST /api/v1| B
  W -->|HTTPS REST /api/v1| B
  A -->|HTTPS REST /api/v1| B
```

| Path | Technology | Role |
|------|------------|------|
| `mobile/` | Kotlin Multiplatform, Compose, Ktor, Koin | Primary product: auth, library, generation, playback, subscriptions |
| `backend/` | Spring Boot 3, Kotlin, JPA, Flyway, Redis | Single REST API under `/api/v1`, integrations |
| `web/` | React, Vite, TypeScript | Parent web; dev proxy to API |
| `admin/` | Next.js 14, Tailwind, Radix | Internal dashboard; BFF-style proxy to API in places |
| `guardrails-service/` (optional submodule) | Python, FastAPI | Optional HTTP sidecar for structured JSON checks |

---

## 3. System context

External systems and data stores the **backend** coordinates. Clients only talk to the API (not directly to most vendors).

```mermaid
flowchart TB
  subgraph clients [Clients]
    Mobile[Mobile]
    Web[Web]
    Admin[Admin]
  end

  subgraph api [Tamixa API]
    BE[Spring Boot]
  end

  subgraph data [Data plane]
    PG[(PostgreSQL)]
    RD[(Redis)]
    S3[(Object storage S3-compatible)]
  end

  subgraph optional [Optional]
    GR[guardrails-service]
  end

  subgraph vendors [External providers examples]
    OAI[OpenAI]
    TTS[Google Cloud TTS / others]
    VC[ElevenLabs / XTTS / Google voice]
    PAY[Stripe]
    MSG[SendGrid / Twilio]
    PUSH[FCM / APNs]
    AV[HeyGen / Replicate / D-ID / etc.]
  end

  Mobile --> BE
  Web --> BE
  Admin --> BE
  BE --> PG
  BE --> RD
  BE --> S3
  BE -.->|feature-flag| GR
  BE --> OAI
  BE --> TTS
  BE --> VC
  BE --> PAY
  BE --> MSG
  BE --> PUSH
  BE --> AV
```

---

## 4. Backend internal architecture (hexagonal / layered)

The backend follows a **modular monolith**: one deployable, boundaries via **ports** (`application.port`) and **adapters** (`infrastructure`).

```mermaid
flowchart TB
  subgraph api_layer [api — com.tamixa.api]
    C[Controllers]
    D[DTOs]
    SEC[Security filters]
  end

  subgraph app_layer [application — com.tamixa.application]
    S[Services / use cases]
    P[Ports interfaces]
  end

  subgraph domain_layer [domain — com.tamixa.domain]
    ENT[Domain models]
  end

  subgraph infra [infrastructure — com.tamixa.infrastructure]
    JPA[JPA adapters]
    R[Redis]
    ST[S3 / CDN clients]
    HTTP[Vendor HTTP clients]
  end

  C --> S
  S --> P
  S --> ENT
  P --> JPA
  P --> R
  P --> ST
  P --> HTTP
```

**Typical request path:** HTTP → controller → application service → port → adapter → DB / Redis / S3 / external API → DTO response.

---

## 5. Use cases (logical)

Actors and major capabilities (not every endpoint). **Parent** is the authenticated principal on mobile/web; **Admin** uses role-gated dashboard APIs.

```mermaid
flowchart LR
  subgraph actors
    PA((Parent))
    AD((Admin))
    SYS((Scheduled / Webhook))
  end

  subgraph uc_parent [Parent-facing]
    U1[Register / login / refresh]
    U2[Manage children and profile]
    U3[Browse library and home feed]
    U4[Generate personalized story]
    U5[Play audio / stream URL]
    U6[Favorites and recommendations]
    U7[Voice upload and cloning job]
    U8[Avatar upload and video job]
    U9[Subscription and usage]
    U10[Consent / export / delete account]
    U11[Reading progress and analytics]
    U12[Educational features quizzes streaks vocabulary]
  end

  subgraph uc_admin [Admin-facing]
    U13[Moderate and approve content]
    U14[Manage library stories and pipeline]
    U15[Users parents audit]
    U16[Health and operational views]
  end

  subgraph uc_system [System]
    U17[Stripe webhooks]
    U18[Push notifications device tokens]
  end

  PA --> U1
  PA --> U2
  PA --> U3
  PA --> U4
  PA --> U5
  PA --> U6
  PA --> U7
  PA --> U8
  PA --> U9
  PA --> U10
  PA --> U11
  PA --> U12

  AD --> U13
  AD --> U14
  AD --> U15
  AD --> U16

  SYS --> U17
  SYS --> U18
```

---

## 6. Sequence: personalized story generation (simplified)

High-level flow; actual steps vary by **story source** (generated vs library) and feature flags.

```mermaid
sequenceDiagram
  participant App as Mobile / Web
  participant API as Spring API
  participant Svc as StoryService / Orchestrator
  participant AI as LLM provider
  participant Mod as Safety / guardrails
  participant DB as PostgreSQL

  App->>API: POST generate story (auth JWT)
  API->>Svc: Validate limits, ownership
  Svc->>Mod: Moderation / policy checks
  Mod-->>Svc: Pass / reject
  Svc->>AI: Generate structured story
  AI-->>Svc: Story content
  Svc->>DB: Persist story + status
  Svc-->>API: Story id / status
  API-->>App: Response
```

Narration (TTS), translation, and avatar pipelines are often **asynchronous jobs** tracked via entities such as `processing_job` and related services—see [backend/NARRATION_ARCHITECTURE.md](backend/NARRATION_ARCHITECTURE.md).

---

## 7. Sequence: playback (stream URL)

```mermaid
sequenceDiagram
  participant App as Mobile
  participant API as StreamUrlController
  participant PB as PlaybackManifestService
  participant ST as Storage / CDN

  App->>API: GET stream URL or timeline (JWT)
  API->>PB: Resolve story + language + voice
  PB->>ST: Signed URL or manifest assembly
  PB-->>API: URLs + metadata
  API-->>App: StreamUrlResponse / manifest
  App->>ST: GET audio (direct or proxied)
```

---

## 8. API surface (domains)

REST is versioned under **`/api/v1`**. Illustrative controller areas (see `com.tamixa.api` for the full set):

| Domain | Examples |
|--------|----------|
| Auth | `AuthController` |
| Stories | `StoryController`, `StoriesApiController`, `LibraryStoryController` |
| Playback / stream | `StreamUrlController`, `AudioStreamProxyController`, `NarrationController` |
| Voice / avatar | `VoiceController`, `VoiceCloningController`, `FamilyAvatarController` |
| Subscription | `SubscriptionController`, webhooks |
| Parent / profile | `ProfileController`, `HomeController` |
| Analytics / progress | `PlaybackPositionController`, `StoryAnalyticsController`, `ListeningProgressController` |
| Admin | `AdminController`, `AdminUserController` |
| Education | `QuizController`, `ReadingLevelController`, `ReadingStreakController`, `VocabularyController`, `TeacherController` |
| Compliance | `ConsentController`, `DataExportController` |
| Ops | `HealthController`, `DeviceController` |

Swagger/OpenAPI is available in dev as described in the root [README.md](../README.md).

---

## 9. Conceptual data model (ER sketch)

Not every table is shown; this captures core relationships for storytelling.

```mermaid
erDiagram
  PARENT ||--o{ CHILD : has
  PARENT ||--o{ VOICE_PROFILE : owns
  PARENT ||--o{ SUBSCRIPTION : has
  CHILD ||--o{ STORY : personalized_for
  LIBRARY_STORY ||--o{ STORY_TRANSLATION : localized_as
  STORY_TRANSLATION ||--o{ NARRATION_AUDIO : spoken_as
  PARENT ||--o{ FAVORITE : saves
```

Exact table names match Flyway migrations under `backend/src/main/resources/db/migration/`.

---

## 10. Security model (high level)

```mermaid
flowchart LR
  subgraph client [Client]
    T[JWT access + refresh]
  end

  subgraph api [API]
    F[JwtAuthenticationFilter]
    R[Role checks ADMIN vs PARENT]
    RL[Rate limiting Redis / in-memory]
  end

  subgraph rules [Rules]
    O[Resource ownership parent owns child/story]
    V[Input validation DTOs]
  end

  client --> F
  F --> R
  R --> O
  F --> RL
```

- **Admin** routes require elevated roles/permissions (see `AdminAuth`, `AdminPermission`).
- **Secrets** come from environment variables; never commit `.env` (see [.env.example](../.env.example) and [ENV_REFERENCE.md](ENV_REFERENCE.md)).

---

## 11. Deployment view (generic)

```mermaid
flowchart TB
  subgraph users [Users]
    U[Mobile and browsers]
  end

  subgraph edge [Edge optional]
    CDN[CDN signed media]
    LB[Load balancer]
  end

  subgraph compute [Compute]
    API1[API instance]
    API2[API instance]
  end

  subgraph data [Managed services]
    PG[(PostgreSQL)]
    RD[(Redis)]
    OB[(Object storage)]
  end

  U --> LB
  LB --> API1
  LB --> API2
  U --> CDN
  CDN --> OB
  API1 --> PG
  API1 --> RD
  API1 --> OB
  API2 --> PG
  API2 --> RD
  API2 --> OB
```

Concrete AWS/GCP steps live in [DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md), [PRODUCTION_UPGRADE.md](PRODUCTION_UPGRADE.md), and related plans.

---

## 12. Mobile navigation vs backend (reference)

The mobile app’s **screens and API alignment** are documented in [ARCHITECTURE_AND_FLOWS.md](ARCHITECTURE_AND_FLOWS.md) (route → `*Controller`). Folder layout: [mobile/FOLDER_STRUCTURE.md](mobile/FOLDER_STRUCTURE.md).

---

## 13. Documentation map

| Topic | Document |
|--------|----------|
| Integrations and optional guardrails | [ARCHITECTURE.md](ARCHITECTURE.md) |
| Narration / TTS pipeline | [backend/NARRATION_ARCHITECTURE.md](backend/NARRATION_ARCHITECTURE.md) |
| Voice cloning | [VOICE_CLONING_ARCHITECTURE.md](VOICE_CLONING_ARCHITECTURE.md) |
| Avatar video | [AVATAR_VIDEO.md](AVATAR_VIDEO.md) |
| Security | [SECURITY.md](SECURITY.md) |
| Env vars | [ENV_REFERENCE.md](ENV_REFERENCE.md) |
| AI guardrails | [AI_GUARDRAILS.md](AI_GUARDRAILS.md) |
| Admin UI / pipeline | [admin/](admin/) |
| Cursor / agent rules | [AGENTS.md](../AGENTS.md), [CURSOR_AGENTS_GUIDE.md](CURSOR_AGENTS_GUIDE.md) |

---

## 14. Revision note

Implementation evolves; if a diagram disagrees with code, **code and Flyway migrations win**. Prefer updating this file when you add major domains (new controller packages, new clients, or deployment topology changes).
