# Tamixa Backend — Modular Monolith Structure

**Version:** 1.0  
**Last Updated:** 2025-03-14

---

## Overview

Tamixa backend is a **modular monolith** in Spring Boot (Kotlin): one deployable, clean module separation, async job workers in-process, designed for easy migration to microservices later.

---

## Ideal vs Current Module Mapping

| Ideal Module | Package / Implementation | Status |
|--------------|--------------------------|--------|
| **common** | `com.tamixa.common` (JobType, JobState) | ✅ Added |
| **auth** | `api.auth`, `application.auth` | ✅ Present |
| **users** | `infrastructure.persistence` (ParentEntity, ChildEntity) | ✅ Parents/children |
| **stories** | `application.story`, `api.story` | ✅ Present |
| **storylibrary** | `application.storylibrary`, `application.library`, `api.library` | ✅ Present |
| **storypipeline** | `application.narration`, `infrastructure.pipeline` | ✅ Present |
| **review** | `StoryModerationService`, Story for review workflow | ✅ Content review |
| **tts** | `application.narration`, `TTSService`, adapters | ✅ Present |
| **voiceclone** | `application.voice`, `infrastructure.voice` | ✅ Present |
| **avatars** | `application.avatar`, `api.avatar` | ✅ Present |
| **talkingvideo** | `AvatarVideoService`, Gooey/D-ID adapters | ✅ Present |
| **playback** | `application.playback`, `api.stream`, `PlaybackManifestService` | ✅ Present |
| **continuation** | `PlaybackPositionService`, HomeService.continueAdventure | ✅ Continue Adventure |
| **sharing** | `application.shareclip`, `shareable_clips` | ✅ Present |
| **subscriptions** | `application.subscription`, Stripe adapters | ✅ Present |
| **analytics** | `StoryAnalyticsService`, `StoryAnalyticsEntity` | ✅ Present |
| **jobs** | `processing_job`, `ProcessingJobService`, executors | ✅ Present |
| **admin** | `AdminController`, `AdminService` | ✅ Present |

---

## Package Layout (`com.tamixa`)

```
com.tamixa
 ├── TamixaApplication.kt
 ├── common                    # Shared enums, job types
 │    ├── JobType.kt
 │    └── JobState.kt
 ├── api                       # Controllers, DTOs
 │    ├── auth, home, library, playback, stream, story, subscription, voice, ...
 │    ├── config, exception
 │    └── admin
 ├── application               # Business logic
 │    ├── account, auth, avatar, home, library, narration, playback
 │    ├── story, storylibrary, stream, subscription, voice
 │    ├── shareclip, translation, admin, analytics
 │    └── port                  # Port interfaces (hexagonal)
 ├── domain                    # Domain types
 │    ├── narration
 │    ├── Story, LibraryStory, Subscription
 │    └── ...
 └── infrastructure            # External implementations
      ├── persistence, storage, cdn
      ├── narration, voice, openai
      ├── pipeline, redis
      └── stripe, avatar, ...
```

---

## Request Flow (Per Spec)

### A. Home Request

- **GET** `/api/v1/home` or `/api/v1/stories/home`
- **Response:** `continueAdventure`, `recommended`, `popular`, `categories`, `familyVoiceStories`

### B. Play Story (Timeline)

- **GET** `/api/v1/stories/{id}/timeline?language=ta&storySource=library&voiceProfile=default`
- **Response:** Playback manifest with `scenes`, `segments`, `audioUrl`, `totalDurationMs`
- Segments include `speaker`, `text`, `durationMs` for subtitle sync

### C. Stream URL

- **GET** `/api/v1/stories/{id}/stream-url?language=ta&voiceProfile=...`

### D. Upload Voice

- **POST** `/api/v1/voice/profiles` (voice cloning)

### E. Upload Avatar

- **POST** `/api/v1/parents/me/avatar`

### F. Create Talking Video

- Via `AvatarVideoService`; async job for lip-sync generation

---

## Job Types (`com.tamixa.common.JobType`)

| Type | Purpose |
|------|---------|
| `STORY_PIPELINE` | Library story: translate + TTS |
| `STORY_POLISH_JOB` | AI polish (future) |
| `STORY_TRANSLATION_JOB` | Translation only (future) |
| `STORY_TTS_JOB` | TTS only (future) |
| `VOICE_CLONE_JOB` | Voice cloning (voice_cloning_jobs) |
| `TALKING_VIDEO_JOB` | Avatar talking video |
| `SHARE_CLIP_JOB` | Share clip generation |

---

## Process Summary

1. **Story created/imported** → library or generated
2. **AI polish + translation** → `StoryProcessingService`
3. **Content manager review** → Story for review, Approve for delivery
4. **Approval** → Enqueue pipeline
5. **Default TTS generation** → `story_narration_audio`
6. **Playback timeline packaging** → `PlaybackManifestService`
7. **Story published to library**
8. **User browses and listens** → Home, timeline, stream URL
9. **Optional** cloned voice personalization
10. **Optional** avatar talking video

---

## Async Processing

- **Executor pools:** `triggerPipelineExecutor`, `pipelineLanguageExecutor`, `pipelineTtsExecutor`, `narrationTtsExecutor`
- **Job tracking:** `processing_job` table, `ProcessingJobService`
- **Voice cloning:** `VoiceCloningJobEntity`, `@Async processVoiceCloningJob`
- **Share clips:** `ShareableClipEntity`, `ShareClipService` (FFmpeg)
- **Data export:** `DataExportJobEntity`, `DataExportProcessor` (`@Scheduled`)

---

## Media Storage

- **S3:** voice samples, generated audio, avatar images, talking videos, share clips
- **CDN:** playback audio, images (optional `CDN_STREAM_ENABLED`)
