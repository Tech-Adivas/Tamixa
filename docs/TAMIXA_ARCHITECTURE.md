# Tamixa Platform — System Architecture

**Version:** 1.0  
**Last Updated:** 2025-03-13  
**Status:** Production-ready design; implementation partially complete

---

## Executive Summary

Tamixa is an AI-powered storytelling platform supporting:

- **AI-assisted story creation** (polish, translate, moderate)
- **Multi-language story management**
- **Admin approval workflow**
- **Text-to-Speech** (Google Cloud TTS, OpenAI, Tamixa voices)
- **Personalized voice cloning** (ElevenLabs, HeyGen, XTTS)
- **Avatar storytelling videos** (HeyGen, Replicate SadTalker, D-ID, Gooey)
- **Mobile-first consumption** (KMP + Jetpack Compose)

The architecture follows a **modular monolith** approach: single deployable with clear domain boundaries, designed to scale horizontally and optionally split into microservices later.

---

## 1. System Architecture

### 1.1 High-Level Diagram

```
                                    ┌──────────────────────────────────────────────────────┐
                                    │                    CDN (CloudFront)                   │
                                    │           Signed URLs / Media Delivery                 │
                                    └─────────────────────┬────────────────────────────────┘
                                                           │
┌─────────────┐    ┌─────────────┐    ┌──────────────────▼────────────────────────────────┐
│   Mobile    │    │   Admin     │    │              API Layer (Spring Boot)                │
│  (KMP)      │    │  (Next.js)  │    │  JWT Auth │ Rate Limit │ CORS │ Request Tracing    │
└──────┬──────┘    └──────┬──────┘    └──────────────────┬────────────────────────────────┘
       │                  │                              │
       │                  │         ┌─────────────────────┼─────────────────────┐
       │                  │         │                     │                     │
       │                  │    ┌────▼────┐  ┌──────▼──────▼────┐  ┌────▼───────┐ │
       │                  │    │  Auth   │  │  Story / Admin   │  │   Stream   │ │
       │                  │    │ Service │  │   Controllers    │  │  (Proxy)   │ │
       │                  │    └────┬────┘  └──────┬───────────┘  └──────┬─────┘ │
       │                  │         │              │                      │       │
       └──────────────────┴────────┴──────────────┴──────────────────────┴───────┘
                                    │
         ┌──────────────────────────┼──────────────────────────────────────────┐
         │                          │              Application Layer            │
         │  ┌───────────────────────┼───────────────────────────────────────┐  │
         │  │ StoryLibraryService   │ StoryProcessingService │ AdminService  │  │
         │  │ TranslationService   │ TTSService            │ AvatarVideo   │  │
         │  │ VoiceCloningService  │ FamilyAvatarService   │ AudioStream   │  │
         │  └───────────────────────┼───────────────────────────────────────┘  │
         │                          │                                           │
         │  ┌───────────────────────▼───────────────────────────────────────┐  │
         │  │               Job Queue (Kafka / Redis Queue)                   │  │
         │  │  approve-narration → StoryProcessingService  │ narration-request (on-demand) │  │
         │  │  voice-cloning-job     │ avatar-video-job    │ (future)        │  │
         │  └───────────────────────────────────────────────────────────────┘  │
         └──────────────────────────────────────────────────────────────────────┘
                                    │
         ┌──────────────────────────┼──────────────────────────────────────────┐
         │                          │           Infrastructure Layer            │
         │  ┌─────────────┐  ┌──────▼──────┐  ┌─────────────┐  ┌────────────┐  │
         │  │ PostgreSQL  │  │   Redis     │  │   AWS S3    │  │ External AI │  │
         │  │ (Flyway)    │  │ Translation │  │ Audio/Video │  │ OpenAI/TTS  │  │
         │  │             │  │ TTS Cache   │  │ Cover/Export│  │ ElevenLabs  │  │
         │  │             │  │ Story Cache │  │ Avatar      │  │ HeyGen      │  │
         │  └─────────────┘  └─────────────┘  └─────────────┘  └────────────┘  │
         └──────────────────────────────────────────────────────────────────────┘
```

### 1.2 Component Mapping (Current Implementation)

| Component       | Implementation                                   | Notes                                          |
|----------------|---------------------------------------------------|------------------------------------------------|
| API Gateway    | Spring Boot + Security + JWT                      | Single API; consider Kong/AWS API GW for scale |
| Auth Service   | `AuthController`, `JwtAuthenticationFilter`      | Email/password, OTP, magic link                 |
| Story Service  | `StoryLibraryService`, `StoryProcessingService`   | Curated + AI-generated stories                 |
| AI Service     | OpenAI (story, moderation), Translation, Rewrite  | In-process; can extract to worker              |
| Voice Service  | `TTSService`, `VoiceCloningService`, `ElevenLabs` | Google TTS, ElevenLabs, HeyGen, XTTS            |
| Avatar Service | `AvatarVideoService`, `FamilyAvatarService`       | HeyGen primary; Replicate/D-ID/Gooey fallback   |
| Media Service  | S3 adapters, `AudioStreamService`, signed URLs    | S3 only; GCS adapter optional                  |
| Job Queue      | **Inline** (`AsyncStoryPipelineExecutor`, `StoryProcessingService`) | Narration via publishNarrationRequest; curated TTS via approve-narration |
| Object Storage | AWS S3                                           | Audio, cover, avatar, export                    |
| CDN            | Optional (`CDN_STREAM_ENABLED`)                   | CloudFront or similar for signed URLs          |

### 1.3 Microservices vs Modular Monolith — Decision

**Recommendation: Modular Monolith (current) with clear boundaries**

| Factor                | Modular Monolith (Current)                         | Microservices                                 |
|-----------------------|----------------------------------------------------|-----------------------------------------------|
| Team size             | 1–2 backend engineers                              | 5+                                            |
| Deployment            | Single artifact, simpler CI/CD                     | Multiple deployables, service mesh             |
| Local dev             | Single process                                     | Docker Compose / K8s                          |
| Transaction boundaries| Single DB, ACID across domains                     | Saga/outbox patterns                           |
| Observability         | Centralized logs, Prometheus                       | Distributed tracing (Jaeger)                   |
| Scale                 | Horizontal scaling of monolith                     | Independent scaling per service               |

**When to split:**

- Story/AI processing saturates CPU → extract **AI Worker** (Kafka consumer)
- Voice cloning SLA diverges → extract **Voice Service**
- Avatar video processing blocks other requests → extract **Avatar Worker**

---

## 2. Database Design

### 2.1 Schema Overview (PostgreSQL)

The following tables exist or are proposed. **Bold** = exists; *Italic* = proposed.

| Table                  | Purpose                                       | Key Relations                     |
|------------------------|-----------------------------------------------|-----------------------------------|
| **parents**            | User accounts (email, role)                    | children, subscriptions           |
| **children**           | Child profiles (age, interests, avatar)        | parent_id                         |
| **curated_stories**    | Admin-created stories (Tamil source)          | story_translations               |
| **story_translations**  | Per-language content + pipeline status        | master_story_id, narration_scripts|
| **story_narration_scripts** | AI-formatted narration script          | translation_id                    |
| **story_narration_audio**   | Neural TTS per (translation, voice)     | translation_id                    |
| **voice_profiles**     | Cloned voice (ElevenLabs, HeyGen, XTTS IDs)   | parent_id                         |
| **voice_cloning_job**  | Voice cloning async job status                | parent_id, voice_profile_id       |
| **parent_avatar**      | Avatar image for storytelling video           | parent_id                         |
| **story_avatar_video** | Generated avatar video per (story, parent)    | parent_id                         |
| **processing_job**    | Generic AI job tracking (story-level UX)      | resource_id, job_type             |

### 2.2 Processing Job Table (Implemented)

```sql
-- V46__processing_job.sql
CREATE TABLE IF NOT EXISTS processing_job (
    id BIGSERIAL PRIMARY KEY,
    job_type VARCHAR(50) NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    resource_id VARCHAR(128) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    progress INT NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    error_message TEXT,
    metadata TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_processing_job_resource ON processing_job(resource_type, resource_id);
CREATE INDEX idx_processing_job_status ON processing_job(status);
CREATE INDEX idx_processing_job_created ON processing_job(created_at);
```

### 2.3 Key Indexes (Existing)

- `curated_stories`: language, age, theme, status
- `story_translations`: master_story_id, language, status
- `story_narration_audio`: translation_id, voice_profile, status
- `story_avatar_video`: (story_id, story_source, parent_id, language, voice_profile) UNIQUE

---

## 3. API Design

### 3.1 Admin APIs (Existing + Gaps)

| Method | Path | Purpose | Status |
|--------|------|---------|--------|
| POST | `/api/v1/admin/stories` | Create story (raw content) | ✅ Exists |
| PUT | `/api/v1/admin/stories/{id}` | Update story | ✅ Exists |
| POST | `/api/v1/admin/stories/{id}/generate` | Trigger AI polish + translation | 🔶 Use `trigger-pipeline` |
| POST | `/api/v1/admin/stories/{id}/submit-review` | Mark READY_FOR_REVIEW | 🔶 Via `approve-narration` flow |
| POST | `/api/v1/admin/stories/{id}/approve` | Content Manager approve | ✅ Exists |
| POST | `/api/v1/admin/stories/{id}/generate-tts` | Trigger default TTS | ✅ Exists as `trigger-pipeline` |
| GET | `/api/v1/admin/stories/{id}/pipeline-status` | AI progress | ✅ Exists |
| GET | `/api/v1/admin/jobs/{jobId}` | Generic job status | ✅ Implemented |
| GET | `/api/v1/admin/jobs?resourceType=&resourceId=` | Jobs for resource or recent | ✅ Implemented |

**Admin Story Creation Request (CreateCuratedStoryRequest):**

```json
{
  "title": "The Wise Owl",
  "content": "Raw story text...",
  "theme": "adventure",
  "language": "ta",
  "age": 6,
  "childName": "Child",
  "moral": "Wisdom comes from experience",
  "emotionMode": "CALM",
  "targetLanguages": ["en", "hi"]
}
```

### 3.2 User APIs (Existing)

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/v1/stories` | List curated stories |
| GET | `/api/v1/stories/{id}` | Story detail |
| GET | `/api/v1/stories/{id}/stream-url` | Audio/video stream (voiceProfile param) |
| POST | `/api/v1/voice/upload` | Upload voice sample |
| GET | `/api/v1/voice-cloning/jobs` | Voice cloning job status |
| POST | `/api/v1/voice-cloning/create` | Create voice cloning job |
| GET | `/api/v1/avatar` | Get avatar |
| POST | `/api/v1/avatar` | Upload avatar |

**Cloned audio:** On-demand via `GET /stories/{id}/stream-url?voiceProfile=cloned:{profileId}` — cached in `story_narration_audio`.

**Avatar video:** On-demand via same stream-url; `avatarVideoUrl` returned when ready; cached in `story_avatar_video`.

### 3.3 Request/Response Examples

**POST /api/v1/admin/stories** (Create)

```json
// Request
{
  "title": "The Kind Elephant",
  "content": "Once upon a time...",
  "theme": "kindness",
  "language": "ta",
  "age": 4,
  "moral": "Kindness is rewarded"
}

// Response 201
{
  "id": 42,
  "title": "The Kind Elephant",
  "status": "DRAFT",
  "coverImageUrl": null
}
```

**GET /api/v1/admin/stories/42/pipeline-status** (AI Progress)

```json
{
  "ta": "COMPLETED",
  "hi": "TRANSLATING",
  "en": "PENDING",
  "processing": "hi",
  "overallStatus": "TRANSLATING_LANGUAGES",
  "progress": "33"
}
```

**overallStatus values:** `PENDING` | `TRANSLATING_LANGUAGES` | `TTS_PROCESSING` | `READY_FOR_REVIEW` | `FAILED`

---

## 4. AI Processing Pipelines

### 4.1 AI Process 1 — Story Polish + Translation

```
Admin submits raw story
    → Create curated_story (DRAFT)
    → Create processing_job (STORY_POLISH, PENDING)
    → Enqueue or inline:
        1. AI polish (LLM rewrite) → child-friendly, grammar
        2. Extract title/moral if missing
        3. For each target language:
           - Translate (Redis cache)
           - story_translations
           - story_processing_status per language
    → Update processing_job (COMPLETED) or FAILED
```

**Current:** `StoryProcessingService.processLanguagesInternal` runs inline via `ExecutorService`. Kafka topics (`translation-request`, `tts-request`) are defined but not wired.

### 4.2 AI Process 2 — Default TTS

```
Admin triggers pipeline (trigger-pipeline)
    → For each language:
         - RewriteService (conversational)
         - EmotionTaggingService
         - TTSService (Google/OpenAI)
         - Upload to S3
         - story_narration_audio (voice=default, READY)
```

**Current:** `StoryProcessingService` handles this. Idempotent per (translation, voice).

### 4.3 Voice Cloning Audio (On-Demand + Cache)

```
User requests stream-url?voiceProfile=cloned:123
    → Check story_narration_audio (translation, cloned:123)
    → If READY: return URL
    → Else: StoryProcessingOrchestrator.process with ClonedVoiceStrategy
         - ElevenLabs / HeyGen / XTTS
         - Upload WAV→MP3 to S3
         - Save READY
```

**Cache key:** (translation_id, voice_profile).

### 4.4 Avatar Video Generation

```
User requests avatar video
    → Check story_avatar_video (story_id, parent_id, language, voice_profile)
    → If READY: return storage_path / signed URL
    → Else: AvatarVideoService
         - HeyGen primary; Replicate/D-ID/Gooey fallback
         - Lip-sync: avatar image + narration audio
         - Store in S3
         - Save READY
```

**Cache key:** (story_id, story_source, parent_id, language, voice_profile).

### 4.5 Retry & Failure Handling

| Stage          | Max Retries | Backoff | On Exhaustion |
|----------------|-------------|---------|---------------|
| Translation    | 3           | 2s, 5s, 10s | story_translations.status=TRANSLATION_FAILED |
| TTS            | 3           | 2s, 5s, 10s | TTS_FAILED |
| Voice cloning  | 2           | 5s, 15s | voice_cloning_job FAILED |
| Avatar video   | 2           | 30s, 60s | story_avatar_video status=FAILED |

### 4.6 Actual Story Flow (Implemented)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ FLOW 1 — STORY CREATION & SUBMISSION                                             │
└─────────────────────────────────────────────────────────────────────────────────┘

  Admin creates story (POST /admin/stories)
       │
       ├─ status: DRAFT  → Save only; no pipeline
       │
       └─ status: PUBLISHED  → Submit for review
            │
            ├─ Pipeline state is RESET (translations cleared, audio deleted)
            ├─ Story appears in "Story for Review" queue
            └─ pipeline-status shows PENDING (no translations yet)

  Admin can optionally:
       • regenerateCover (DALL-E) before or after submit
       • use "Regenerate with prompt" on the edit page to transform raw text via LLM (preview only; save or submit to persist)

┌─────────────────────────────────────────────────────────────────────────────────┐
│ FLOW 2 — APPROVAL & TTS GENERATION                                               │
└─────────────────────────────────────────────────────────────────────────────────┘

  Content Manager opens "Story for Review"
       │
       ├─ pipeline-status: PENDING → Show "Approve narration" button
       │
       └─ Content Manager clicks "Approve narration" (POST /approve-narration)
            │
            ├─ create processing_job (STORY_PIPELINE, IN_PROGRESS)
            ├─ triggerPipelineExecutor runs processSync/processLanguagesAsync
            │
            └─ For each language (ta, en, hi, ...) in parallel:
                 │
                 ├─ TRANSLATING  → TranslationService (Tamil → target)
                 ├─ REWRITING    → RewriteService (conversational tone)
                 ├─ TTS_PROCESSING → TTSService (Google/OpenAI) → S3
                 └─ COMPLETED    → story_narration_audio READY
            │
            ├─ All done → processing_job COMPLETED, story status READY
            └─ Partial/fail → processing_job FAILED, story stays PUBLISHED

  Admin UI: Poll pipeline-status; disable Approve while overallStatus in
  (TRANSLATING_LANGUAGES, TTS_PROCESSING). When READY_FOR_REVIEW, Approve enabled.

┌─────────────────────────────────────────────────────────────────────────────────┐
│ FLOW 3 — USER LISTENING                                                          │
└─────────────────────────────────────────────────────────────────────────────────┘

  User: GET /stories/{id}/stream-url?language=ta&voiceProfile=default
       → Default narration (pre-generated)
       → voiceProfile=cloned:{id} → on-demand cloned audio (cached)

  User: same URL with avatar → avatarVideoUrl when available (cached)
```

---

## 5. Code Scaffolding (Kotlin Spring Boot)

### 5.1 Current Structure (Modular Monolith)

```
backend/
├── src/main/kotlin/com/tamixa/
│   ├── api/
│   │   ├── controller/        # AdminController, AuthController, ...
│   │   ├── admin/dto/
│   │   ├── config/            # Security, JWT, CORS
│   │   └── exception/
│   ├── application/
│   │   ├── admin/
│   │   ├── avatar/
│   │   ├── narration/
│   │   ├── story/
│   │   ├── stream/
│   │   ├── translation/
│   │   ├── voice/
│   │   ├── pipeline/
│   │   └── port/              # Ports (interfaces)
│   ├── domain/
│   └── infrastructure/
│       ├── persistence/
│       ├── storage/
│       ├── openai/
│       ├── tts/
│       ├── avatar/
│       └── redis/
└── src/main/resources/
    └── db/migration/
```

### 5.2 Logical Modules (Domain Boundaries)

| Module              | Controllers | Services | Repositories |
|---------------------|-------------|----------|--------------|
| auth                | AuthController | AuthService, OtpService | ParentJpaRepository |
| story               | StoryController, AdminController (stories) | StoryLibraryService, StoryProcessingService | CuratedStory*, StoryTranslation* |
| ai                  | (embedded) | TranslationService, RewriteService, EmotionTagging | — |
| voice               | VoiceController, VoiceCloningController | VoiceCloningService, TTSService | VoiceProfile*, VoiceCloningJob* |
| avatar              | FamilyAvatarController | AvatarVideoService, FamilyAvatarService | ParentAvatar*, StoryAvatarVideo* |
| media               | AudioStreamProxyController | AudioStreamService, S3 adapters | — |
| notification        | (future) | EmailSenderPort impl | — |

---

## 6. Job Tracking System

### 6.1 ProcessingJob Table (see §2.2)

### 6.2 Job Status API (Proposed)

```
GET /api/v1/admin/jobs?resourceType=curated_story&resourceId=42
GET /api/v1/admin/jobs/{jobId}
```

**Response:**

```json
{
  "id": 1,
  "jobType": "STORY_POLISH",
  "resourceType": "curated_story",
  "resourceId": "42",
  "status": "IN_PROGRESS",
  "progress": 60,
  "startedAt": "2025-03-13T10:00:00Z",
  "errorMessage": null
}
```

### 6.3 AI Progress UX Statuses

| Status                | Admin UX Meaning                        |
|-----------------------|-----------------------------------------|
| AI_PROCESSING         | LLM polishing raw text                  |
| TRANSLATING_LANGUAGES | Translating into target languages       |
| TTS_PROCESSING        | Generating default narration audio      |
| FINALIZING_STORY      | Finalizing metadata, covers             |
| READY_FOR_REVIEW      | Admin can submit for review            |
| FAILED                | Error; show retry                       |

Admin must **not** submit for review while status is `AI_PROCESSING`, `TRANSLATING_LANGUAGES`, or `TTS_PROCESSING`.

---

## 7. UI/UX Design Suggestions

| Screen                | Suggestions |
|-----------------------|-------------|
| **Admin story creation** | Multi-step form; raw text → target languages → generate; disable submit until AI done |
| **AI progress**       | Progress bar + stage list (AI_PROCESSING, TRANSLATING, FINALIZING); real-time via polling or SSE |
| **Story review**      | Side-by-side: original vs translated; approve/reject/edit per language |
| **Story library**     | Card grid; filter by age, theme, language; play preview |
| **Story player**       | Play/pause, speed, voice selector (default, my voice, favorite person) |
| **Voice cloning flow** | Upload/record 30s minimum; format validation; progress indicator; consent checkbox |
| **Avatar generation**  | Upload photo → preview → generate; show "Generating..." with static avatar + audio fallback |

---

## 8. Technology Recommendations

| Domain            | Recommended Tools | Current / Notes |
|-------------------|-------------------|-----------------|
| Voice cloning     | ElevenLabs, HeyGen, XTTS (self-hosted) | ✅ All integrated |
| Avatar generation | HeyGen (primary), Replicate SadTalker | ✅ HeyGen + fallbacks |
| Lip-sync          | HeyGen, SadTalker, D-ID | ✅ Via providers |
| Video generation  | HeyGen streaming, Replicate | ✅ In use |
| Media streaming   | S3 signed URLs, CloudFront CDN | ✅ S3; CDN optional |
| Queue system      | Kafka (scale), Redis Streams (simpler) | Inline now; Kafka ready |
| Monitoring        | Prometheus + Grafana, Micrometer | ✅ Micrometer |
| Moderation        | OpenAI Moderation API + keyword blocklist | ✅ StoryModerationService |

---

## 9. Performance & Scalability

| Area                | Recommendation |
|---------------------|----------------|
| Cost optimization  | Redis cache for translation/TTS; avoid re-calling APIs for same (story, lang) |
| AI efficiency      | Batch translations where possible; circuit breaker for OpenAI/TTS |
| Media caching      | story_narration_audio, story_avatar_video; CDN for signed URLs |
| CDN delivery        | Enable `CDN_STREAM_ENABLED`; CloudFront in front of S3 |
| Voice cloning scale| Queue voice cloning jobs; limit concurrent TTS to external APIs |
| Avatar video scale | Dedicated worker pool; replicate/D-ID rate limits |

---

## 10. Security

| Concern                    | Mitigation |
|----------------------------|------------|
| Voice impersonation        | Consent required; log clone creation; rate limit clones per user |
| Content moderation         | OpenAI Moderation + blocklist; StorySafetyScoreService |
| Rate limiting              | Bucket4j on auth, story generation; Redis-backed for multi-instance |
| Secure media storage       | S3 private buckets; signed URLs with expiry (e.g. 1h) |
| User consent (voice clone) | parent_consent table; explicit agreement before clone |
| Admin RBAC                 | Roles: ADMIN, SUPER_ADMIN, CONTENT_MANAGER, etc.; @PreAuthorize |

---

## Appendix A: Story Status Workflow

```
curated_stories.status:
  DRAFT       → Admin editing; not in review queue
  PUBLISHED   → In "Story for Review" queue; pipeline may be PENDING or running
  PROCESSING  → Pipeline running (translate/TTS)
  READY       → All languages have audio; ready for user consumption

story_translations.status (per language):
  PENDING → TRANSLATING → REWRITING → TTS_PROCESSING → COMPLETED
  (or TRANSLATION_FAILED | REWRITE_FAILED | TTS_FAILED)

processing_job.status (STORY_PIPELINE):
  IN_PROGRESS → COMPLETED | FAILED
```

## Appendix B: Configuration Summary

| Env Var                    | Purpose |
|----------------------------|---------|
| `DATABASE_URL`             | PostgreSQL |
| `REDIS_HOST`              | Cache |
| `S3_BUCKET`, `AWS_REGION`  | Storage |
| `OPENAI_API_KEY`           | Story, moderation |
| `GOOGLE_CLOUD_TTS_API_KEY` | Default TTS |
| `ELEVENLABS_API_KEY`       | Voice cloning |
| `HEYGEN_API_KEY`           | Avatar + HeyGen TTS |
| `REPLICATE_API_TOKEN`      | SadTalker fallback |

---

*This document aligns with the existing Tamixa implementation and provides a blueprint for scaling and refinement.*
