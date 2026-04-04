# Tamixa Technical Implementation Documentation

**Version:** 1.0  
**Date:** March 2025  

**Related docs:**
- [TAMIXA_MVP_DESIGN.md](./TAMIXA_MVP_DESIGN.md) — Product flows, scope, API design
- [NARRATION_ARCHITECTURE.md](./backend/NARRATION_ARCHITECTURE.md) — TTS pipeline details
- [AVATAR_VIDEO.md](./AVATAR_VIDEO.md) — Avatar video provider config

---

## Table of Contents

1. [Overview](#1-overview)
2. [System Architecture](#2-system-architecture)
3. [Package Structure](#3-package-structure)
4. [Layered Architecture](#4-layered-architecture-details)
5. [Core Services & Data Flows](#5-core-services--data-flows)
6. [Async & Background Jobs](#6-async--background-job-model)
7. [External Integrations](#7-external-integrations)
8. [Security Architecture](#8-security-architecture)
9. [Database Schema](#9-database-schema)
10. [API Reference](#10-api-reference-summary)
11. [Configuration](#11-configuration)
12. [Deployment & Operations](#12-deployment--operations)
13. [Share Clip Implementation](#13-share-clip-implementation-to-build)

---

## 1. Overview

Tamixa is a modular monolith built with **Spring Boot 3**, **Kotlin 1.9**, **PostgreSQL**, and **Redis**. The backend serves:

- **Mobile app** (KMP Compose) — primary client
- **Admin dashboard** (Next.js) — content management
- **Web app** (React/Vite) — parent-facing (optional)

Architecture follows a **hexagonal-style** layering: `api` → `application` → `domain` → `infrastructure`.

---

## 2. System Architecture

### 2.1 High-Level Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────────────────────────┐
│                                    TAMIXA SYSTEM                                             │
└────────────────────────────────────────────────────────────────────────────────────────────┘

                                    ┌─────────────────┐
                                    │   CDN / S3      │
                                    │  (Media URLs)   │
                                    └────────▲────────┘
                                             │
┌──────────────┐    ┌──────────────┐         │         ┌──────────────────────────────────────┐
│  Mobile      │    │   Admin      │         │         │     SPRING BOOT BACKEND              │
│  (KMP)       │────│   (Next.js)  │─────────┼────────►│     :8080 /api/v1                   │
└──────────────┘    └──────────────┘   HTTPS │         │                                      │
       │                    │                │         │  ┌─────────────────────────────────┐ │
       │                    │                │         │  │ Controllers (REST)              │ │
       │                    │                │         │  │ Auth | Stories | Stream | Voice  │ │
       │                    │                │         │  │ Playback | Admin | ShareClip     │ │
       │                    │                │         │  └──────────────┬──────────────────┘ │
       │                    │                │         │                 │                    │
       │                    │                │         │  ┌───────────────▼──────────────────┐ │
       │                    │                │         │  │ Application Services            │ │
       │                    │                │         │  │ StoryLibrary | AudioStream      │ │
       │                    │                │         │  │ AvatarVideo | VoiceCloning      │ │
       │                    │                │         │  │ ShareClip | Playback | etc.     │ │
       │                    │                │         │  └───────────────┬──────────────────┘ │
       │                    │                │         │                 │                    │
       │                    │                │         │  ┌───────────────▼──────────────────┐ │
       │                    │                │         │  │ Ports (Interfaces)                │ │
       │                    │                │         │  │ Repositories | Storage | Clients  │ │
       │                    │                │         │  └───────────────┬──────────────────┘ │
       │                    │                │         │                 │                    │
       │                    │                │         │  ┌───────────────▼──────────────────┐ │
       │                    │                │         │  │ Infrastructure Adapters          │ │
       │                    │                │         │  │ JPA | Redis | S3 | OpenAI | TTS  │ │
       │                    │                │         │  └──────────────────────────────────┘ │
       │                    │                │         └──────────────────┬───────────────────┘
       │                    │                │                            │
       │                    │                │         ┌───────────────────┼───────────────────┐
       │                    │                │         │                   │                   │
       │                    │                │         ▼                   ▼                   ▼
       │                    │                │  ┌─────────────┐   ┌─────────────┐   ┌─────────────────┐
       │                    │                │  │ PostgreSQL  │   │   Redis     │   │  S3 / Object    │
       │                    │                │  │ (primary DB)│   │  (cache)    │   │  Storage        │
       │                    │                │  └─────────────┘   └─────────────┘   └─────────────────┘
       │                    │                │
       │                    │                │  ┌─────────────────────────────────────────────────────┐
       │                    │                │  │ External APIs: Google TTS | OpenAI | ElevenLabs   │
       │                    │                │  │ HeyGen | Replicate | D-ID | Stripe | Zoho          │
       │                    │                │  └─────────────────────────────────────────────────────┘
       └────────────────────┴────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Component Overview

| Layer | Purpose | Key Packages |
|-------|---------|--------------|
| **API** | HTTP controllers, DTOs, validation | `com.tamixa.api.*` |
| **Application** | Business logic, orchestration | `com.tamixa.application.*` |
| **Domain** | Entities, value objects, ports | `com.tamixa.domain.*` |
| **Infrastructure** | Persistence, external clients, config | `com.tamixa.infrastructure.*` |

---

## 3. Package Structure

```
backend/src/main/kotlin/com/tamixa/
├── api/                          # REST API layer
│   ├── ApiVersion.kt             # /api/v1 base path
│   ├── auth/                     # AuthController, DTOs
│   ├── library/                  # LibraryStoryController
│   ├── stream/                   # StreamUrlController, AudioStreamProxyController
│   ├── playback/                 # PlaybackPositionController
│   ├── voice/                    # VoiceController, VoiceCloningController
│   ├── avatar/                   # FamilyAvatarController
│   ├── recommendation/           # StoryRecommendationController
│   ├── story/                    # StoryController, StoriesApiController
│   ├── favorite/                 # FavoriteStoryController
│   ├── subscription/             # SubscriptionController
│   ├── controller/               # AdminController, HealthController
│   └── ...
├── application/                  # Business logic
│   ├── storylibrary/             # StoryLibraryService
│   ├── narration/                # StoryProcessingService, TTSService, RewriteService
│   ├── stream/                   # AudioStreamService, NarrationScriptService
│   ├── avatar/                   # AvatarVideoService, FamilyAvatarService
│   ├── voice/                    # VoiceCloningService, StoryVoicesService
│   ├── playback/                 # PlaybackPositionService
│   ├── recommendation/           # StoryRecommendationService
│   ├── subscription/             # SubscriptionService
│   ├── port/                     # Repository and adapter interfaces
│   └── ...
├── domain/                       # Domain models
│   ├── Parent.kt, Story.kt, LibraryStory.kt
│   ├── narration/                # StoryNarrationScript, EmotionTag
│   └── ...
└── infrastructure/               # Adapters
    ├── config/                   # AppProperties, SecurityConfig, NarrationAsyncConfig
    ├── persistence/              # JPA entities, repositories
    ├── storage/                  # S3 adapters
    ├── narration/                # GoogleCloudTtsClientAdapter, OpenAI adapters
    ├── voice/                    # ElevenLabs, XTTS clients
    ├── avatar/                   # HeyGen, Replicate, D-ID clients
    ├── redis/                    # RedisTemplate, cache adapters
    └── observability/            # Metrics, logging
```

---

## 4. Layered Architecture Details

### 4.1 API Layer

- **Controllers** are thin: validate request, delegate to service, return `ResponseEntity`.
- **DTOs** live under `api/<domain>/dto/`. One DTO per file.
- **Security**: `@PreAuthorize("hasRole('PARENT')")` or `hasRole('ADMIN')` on protected endpoints.
- **Base path**: `${ApiVersion.V1}` = `/api/v1`.

| Controller | Base Path | Auth |
|------------|-----------|------|
| AuthController | /auth | Public (login) or PARENT |
| LibraryStoryController | /stories/library | PARENT |
| StreamUrlController | /stories | PARENT |
| PlaybackPositionController | /playback | PARENT |
| VoiceController | /voice | PARENT |
| FamilyAvatarController | /parents/me/avatar | PARENT |
| AdminController | /admin | ADMIN |

### 4.2 Application Layer

- **Services** contain business logic. They depend on **ports** (interfaces), not concrete adapters.
- **Ports** define contracts: `StoryLibraryRepositoryPort`, `AudioStoragePort`, `TtsClientPort`, etc.
- **No direct JPA or HTTP** in application layer—only port calls.

Example flow:
```
StreamUrlController → AudioStreamService → AudioStoragePort (S3)
                   → StoryNarrationAudioRepositoryPort (DB)
```

### 4.3 Domain Layer

- **Entities** and **value objects** with minimal dependencies.
- **Enums** for statuses: `StoryStatus`, `NarrationAudioStatus`, `TranslationPipelineStatus`.
- No infrastructure imports.

### 4.4 Infrastructure Layer

- **Adapters** implement ports: `StoryLibraryJpaRepositoryAdapter`, `S3AudioStorageAdapter`.
- **Conditional beans**: `@ConditionalOnProperty` for optional features (e.g. S3, voice cloning).
- **External clients**: RestTemplate/WebClient for OpenAI, Google TTS, ElevenLabs, HeyGen, Replicate.

---

## 5. Core Services & Data Flows

### 5.1 Story Library & Playback

```
GET /stories/library
  → LibraryStoryController.list()
  → StoryLibraryService.findByLanguageApprovedOnly()
  → StoryLibraryRepositoryPort.findByLanguageAndNarrationApproved()
  → Returns List<LibraryStoryResponse>

GET /stories/library/{id}/stream-url?language=ta&voiceProfile=default
  → StreamUrlController.getLibraryStreamUrl()
  → AudioStreamService.getLibraryStreamUrl(id, lang, parentId)
      → Resolve audio: library_stories.audio_file_url (source) OR story_narration_audio (translation)
      → Build presigned S3 URL or proxy URL
  → AvatarVideoService.getAvatarVideoUrl() if Premium+ (may trigger async job)
  → FamilyAvatarService.getAvatarUrl() for static avatar
  → Returns StreamUrlResponse(url, avatarUrl, avatarVideoUrl)
```

### 5.2 Content Pipeline (TTS)

```
Admin: POST /admin/stories/{id}/trigger-pipeline
  → AdminController (triggerPipelineExecutor.submit)
  → StoryProcessingService.processSync(masterStoryId)
  → For each language (parallel via pipelineLanguageExecutor):
      1. TranslationService.translate() → story_translations
      2. RewriteService.rewrite() → conversational script
      3. EmotionTaggingService.tag() → EmotionTaggedScript
      4. SSMLBuilderService.buildSSML() → SSML string
      5. TTSService.synthesize() → audio bytes
      6. AudioStorageService.store() → S3
      7. story_narration_audio (translation_id, voice_profile="default")
  → story_library status → READY (all langs) or PUBLISHED (partial)
```

### 5.3 Avatar Video Generation

```
GET stream-url with Premium+ + avatar uploaded
  → AvatarVideoService.getAvatarVideoUrl(storyId, source, parentId, lang, voice)
  → Lookup story_avatar_video (story_id, story_source, parent_id, lang, voice)
  → If READY: return signed URL
  → If MISSING: insert PENDING, @Async trigger generateAvatarVideo()
      → Resolve avatar image URL, narration audio URL
      → HeyGen/Replicate/D-ID: create job, poll until done
      → Download video, upload to S3 via StoryAvatarVideoStoragePort
      → Update status READY
  → Client: first request returns null (play static + audio); next request returns video URL
```

### 5.4 Share Clip (MVP)

```
POST /stories/{id}/share-clip { startSeconds, durationSeconds, format }
  → ShareClipController (to be added)
  → ShareClipService.requestClip()
      → SubscriptionGuard: ensure Premium+, check quota (e.g. 5–10/month)
      → Insert shareable_clips (PENDING)
      → @Async trigger ShareClipJob
          → Extract audio segment (FFmpeg or similar)
          → Get/create avatar video segment (or reuse segment from full video)
          → Composite: audio + video + "Made with Tamixa" watermark
          → Upload to S3
          → Update shareable_clips status READY, storage_path
  → Return { clipId, jobId, status: "PENDING" }

GET /share-clips/{clipId}
  → Returns { status, downloadUrl? }
```

---

## 6. Async & Background Job Model

### 6.1 Executor Configuration

| Executor | Bean Name | Purpose |
|----------|-----------|---------|
| getAsyncExecutor | (default) | @Async methods; processAsync, VoiceCloning, AvatarVideo |
| triggerPipelineExecutor | triggerPipelineExecutor | Admin trigger pipeline; runs processSync in background |
| pipelineLanguageExecutor | pipelineLanguageExecutor | Parallel language processing (2–8 threads) |
| pipelineTtsExecutor | pipelineTtsExecutor | Single-threaded TTS serialization |
| narrationTtsExecutor | narrationTtsExecutor | Narration TTS for generated stories |

### 6.2 Job Types & Triggers

| Job | Trigger | Executor | Blocking |
|-----|---------|----------|----------|
| STORY_PIPELINE | Admin trigger | triggerPipelineExecutor | No (async) |
| VOICE_CLONE | POST /voice/upload | @Async | No |
| AVATAR_VIDEO | First playback with avatar | @Async | No |
| SHARE_CLIP | POST /share-clip | @Async (to add) | No |
| RetryFailedStoriesJob | @Scheduled(30 min) | — | No |
| DataExportProcessor | @Scheduled(2 min) | — | No |

### 6.3 Job State Model

```
processing_job / shareable_clips / story_avatar_video:
  PENDING → IN_PROGRESS / PROCESSING → COMPLETED / READY
                                    → FAILED
```

### 6.4 Retry Policy

- **Story pipeline**: max 3 retries per language; exponential backoff 2s, 5s, 10s.
- **Avatar video**: 1 retry on transient failure.
- **Share clip**: 1 retry on FFmpeg/storage failure.

---

## 7. External Integrations

| Service | Purpose | Config |
|---------|---------|--------|
| **Google Cloud TTS** | Default neural narration | NARRATION_TTS_PROVIDER=google, GOOGLE_CLOUD_TTS_API_KEY |
| **OpenAI** | LLM / translation / moderation when `AI_LLM_PROVIDER=openai` | OPENAI_API_KEY, app.openai.baseUrl |
| **Google Gemini** | LLM / translation / cover still image / Veo when configured | GEMINI_API_KEY, `AI_LLM_PROVIDER`, `AI_COVER_IMAGE_PROVIDER`, `COVER_ANIMATION_VEO_ENABLED` |
| **ElevenLabs** | Voice cloning (IVC) | VOICE_CLONING_ENABLED, ELEVENLABS_API_KEY |
| **XTTS** | Self-hosted voice clone | app.voiceCloning.xttsBaseUrl |
| **HeyGen** | Avatar video (primary) | AVATAR_VIDEO_PROVIDER=heygen, HEYGEN_API_KEY |
| **Replicate (SadTalker)** | Avatar fallback | AVATAR_VIDEO_PROVIDER=sadtalker, REPLICATE_API_TOKEN |
| **D-ID** | Avatar fallback | AVATAR_VIDEO_PROVIDER=d-id, DID_API_KEY |
| **S3** | Audio, avatars, videos, covers | STORAGE_TYPE=s3, S3_BUCKET, AWS_* |
| **Stripe** | Subscriptions | app.subscription.stripe.* |
| **Zoho** | Payments (India) | app.subscription.zoho.* |

---

## 8. Security Architecture

### 8.1 Authentication

- **JWT**: Access token (15 min) + refresh token (7 days).
- **Roles**: PARENT, ADMIN.
- **Filter**: JwtAuthenticationFilter validates token on every request.

### 8.2 Authorization

- `@PreAuthorize("hasRole('PARENT')")` — parent-facing endpoints.
- `@PreAuthorize("hasRole('ADMIN')")` — admin endpoints.
- **Resource ownership**: Services verify parent owns child/story/voice before returning.

### 8.3 Premium Gating

- `SubscriptionGuard.enforcePremiumVoiceAccess()` — 402 if not entitled.
- Avatar video: return null when not Premium+; no URL exposed.
- Share clip: 402 if not Premium+ or quota exceeded.

### 8.4 Security Rules (from .cursor/rules)

- No secrets in logs.
- Env for secrets; `.env` gitignored.
- Input validation on all DTOs.
- CORS restrict origins in production.

---

## 9. Database Schema

### 9.1 Entity Relationship (Simplified)

```
parents ──┬── children
          ├── subscription
          ├── voice_profiles ── voice_cloning_jobs
          ├── parent_avatar
          ├── story_playback_position
          ├── favorite_story
          └── shareable_clips (MVP)

library_stories ──┬── story_translations ──┬── story_narration_scripts
                 │                         └── story_narration_audio
                 └── story_avatar_video (per parent)

stories (generated) ── parent_id
```

### 9.2 Key Tables

| Table | Purpose |
|-------|---------|
| library_stories | Curated stories; narration_approved_at gates app visibility |
| story_translations | Per-language content; status for pipeline |
| story_narration_audio | TTS output per (translation, voice_profile) |
| story_avatar_video | Cached talking-head video per (story, parent, lang, voice) |
| shareable_clips | Premium+ clip generation; PENDING→READY |
| processing_job | Generic job tracking for admin UX |

### 9.3 Migrations

- **Flyway** in `backend/src/main/resources/db/migration/`.
- Naming: `V{NN}__description.sql`.
- Latest: V49 `shareable_clips`.

---

## 10. API Reference Summary

| Area | Endpoints |
|------|-----------|
| **Auth** | POST /auth/register, /auth/login, /auth/refresh, GET /auth/me |
| **Library** | GET /stories/library, GET /stories/library/{id} |
| **Stream** | GET /stories/library/{id}/stream-url, GET /stories/{id}/stream-url |
| **Playback** | GET/POST /playback/position, GET /playback/recent |
| **Voice** | POST /voice/upload, GET /voice/profiles |
| **Avatar** | POST/DELETE /parents/me/avatar |
| **Share** | GET /stories/library/{id}/share-url, POST /stories/{id}/share-clip, GET /share-clips/{id} |
| **Admin** | CRUD /admin/stories, POST trigger-pipeline, approve-narration |

Full OpenAPI: `/v3/api-docs` (SpringDoc).

---

## 11. Configuration

### 11.1 Key Properties (application.yml + .env)

| Property | Default | Purpose |
|----------|---------|---------|
| SPRING_PROFILES_ACTIVE | dev | Profile |
| SERVER_PORT | 8080 | HTTP port |
| REDIS_HOST / REDIS_PORT | localhost:6379 | Redis |
| JWT_SECRET | — | Required for prod |
| OPENAI_API_KEY | — | When `AI_LLM_PROVIDER=openai` (and DALL·E covers when `AI_COVER_IMAGE_PROVIDER=openai`) |
| GEMINI_API_KEY | — | When `AI_LLM_PROVIDER=gemini`, Gemini translation, Gemini covers, and/or Veo GIF |
| AI_LLM_PROVIDER / AI_COVER_IMAGE_PROVIDER | openai | Primary LLM and still-cover image backends |
| GOOGLE_CLOUD_TTS_API_KEY | — | Default TTS |
| S3_BUCKET, AWS_ACCESS_KEY_ID | — | Storage |
| AVATAR_VIDEO_PROVIDER | sadtalker | heygen, sadtalker, d-id |
| VOICE_CLONING_ENABLED | false | ElevenLabs/XTTS |

### 11.2 AppProperties Structure

- `app.auth`, `app.storage`, `app.jwt`, `app.openai`, `app.llm`, `app.imageGeneration`, `app.coverAnimation`
- `app.translationPipeline` (sourceLanguage, targetLanguages, parallelism)
- `app.narration` (maxConcurrentTts, ttsTimeoutSeconds)
- `app.avatarVideo`, `app.voiceCloning`, `app.subscription`

---

## 12. Deployment & Operations

### 12.1 Build & Run

```bash
# Backend
./gradlew :backend:bootRun -Ptamixa.backendOnly=true

# Tests
./gradlew :backend:test -Ptamixa.backendOnly=true
```

### 12.2 Health Endpoints

- **Liveness**: `/actuator/health/liveness`
- **Readiness**: `/actuator/health/readiness` (db, redis, diskSpace)
- **Metrics**: `/actuator/prometheus`

### 12.3 Observability

- **Logging**: SLF4J; `com.tamixa` at DEBUG.
- **Metrics**: Micrometer + Prometheus; `tts_concurrency_current`, pipeline duration.
- **MDC**: masterStoryId, language for pipeline traceability.

---

## 13. Share Clip Implementation (To Build)

### 13.1 New Components

| Component | Path | Purpose |
|-----------|------|---------|
| ShareClipController | api/share/ShareClipController.kt | POST share-clip, GET status, GET quota |
| ShareClipService | application/shareclip/ShareClipService.kt | requestClip, getStatus, getQuota, async job |
| ShareableClipRepositoryPort | application/port/ | CRUD shareable_clips |
| ShareableClipJpaRepository | infrastructure/persistence/ | JPA |
| ShareClipJobRunner | application/shareclip/ | @Async clip render: FFmpeg composite |

### 13.2 Clip Render Flow (Technical)

1. **Input**: storyId, parentId, startSeconds, durationSeconds, format (9:16 | 1:1)
2. **Resolve**: Audio URL (from story_narration_audio or full story audio), avatar image URL
3. **Extract**: FFmpeg `-ss startSeconds -t durationSeconds` on audio
4. **Avatar segment**: Option A) Extract from full story_avatar_video if exists; Option B) Generate short clip via HeyGen/Replicate with segment audio
5. **Composite**: FFmpeg overlay "Made with Tamixa" watermark
6. **Output**: MP4 to S3 `share_clips/{parentId}/{clipId}.mp4`
7. **Update**: shareable_clips.status = READY, storage_path

### 13.3 Quota Enforcement

- Query `shareable_clips` count where parent_id = X and created_at >= start of current month.
- If count >= limit (e.g. 10), return 402 with upgrade message.
- Config: `app.shareClip.clipsPerMonthPremiumPlus = 10`

---

## Appendix A: File Reference

| Area | Path |
|------|------|
| Main application | backend/src/main/kotlin/com/tamixa/TamixaApplication.kt |
| API version | backend/.../api/ApiVersion.kt |
| Config | backend/.../infrastructure/config/ |
| Migrations | backend/src/main/resources/db/migration/ |
| Application config | backend/src/main/resources/application.yml |
| Narration docs | docs/backend/NARRATION_ARCHITECTURE.md |
| Avatar docs | docs/AVATAR_VIDEO.md |

---

## Appendix B: Sequence Diagram (Stream URL)

```
Client                StreamUrlController    AudioStreamService    AvatarVideoService    S3
  |                           |                       |                      |            |
  |-- GET stream-url -------->|                       |                      |            |
  |                           |-- getLibraryStreamUrl() ->|                  |            |
  |                           |                       |-- DB lookup ------> |            |
  |                           |                       |<-- audio path ------|            |
  |                           |                       |-- presign ---------> |            |
  |                           |                       |<-- URL -------------|            |
  |                           |-- getAvatarVideoUrl() ------------------->  |            |
  |                           |                       |                   |-- cache? ---> |
  |                           |                       |                   |<-- READY/--  |
  |                           |                       |                   |    null      |
  |                           |<-- StreamUrlResponse -|                   |            |
  |<-- 200 { url, avatarUrl,  |                       |                      |            |
  |      avatarVideoUrl } ----|                       |                      |            |
```
