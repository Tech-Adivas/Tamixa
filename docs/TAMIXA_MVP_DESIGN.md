# Tamixa MVP Design: Phase 1 Storytelling App

**Version:** 1.0  
**Date:** March 2025  
**Author:** CTO / Product Engineering  

---

## Executive Summary

**Recommendation: IMPROVE the current flow.** The existing architecture already implements the core pillars: curated library, approval workflow, TTS pipeline, voice cloning, avatar video, playback, and recommendations. The main work is to **simplify, reorder, and harden for launch**—not replace.

**Key decisions:**
- Story library is primary; AI story generation is optional/secondary
- Pre-generated audio for instant playback; personalized assets async
- **Share clip (Instagram/YouTube-ready video) is IN MVP** — Premium+ users can create shareable clips to attract millions via viral growth
- Single modular Spring Boot backend; KMP mobile (not Flutter—existing investment)
- Launch-ready in 30 days with strict scope

---

## 1. Final Recommended MVP Product Flow

### 1.1 Flow Decision: Improve vs Replace

| Option | Assessment |
|--------|------------|
| **Improve** | ✅ **RECOMMENDED**. Current flow already has: library_stories, story_translations, narration pipeline, approval, voice cloning, avatar video, playback, recommendations. Gaps are: Home screen data model, share clip generation, clearer story continuation UX, and stricter scope. |
| **Replace** | ❌ Would discard working pipeline, TTS, voice, avatar integrations. High risk for 30-day launch. |

### 1.2 Final End-to-End Content Flow

```
Admin creates story (Tamil) → Save as DRAFT
    → Add to library_stories
    → Admin triggers pipeline (translate + rewrite + TTS)
    → StoryProcessingService runs per language:
        Translate (OpenAI) → Rewrite (LLM) → Emotion tag → SSML → TTS → S3
    → Status: PUBLISHED (all languages) or PROCESSING
    → Admin reviews narration in "Story for review"
    → Admin approves → narration_approved_at set
    → Story surfaces in GET /stories/library (approved only)
    → Pre-generated audio ready for instant playback
```

### 1.3 Final End-User Flow

```
Login/Register → Language selection → Dashboard
    → Home: Continue Adventure | Recommended | Popular | Categories | (optional) Tell me a story
    → Browse library (category, search)
    → Tap story → GET /stories/{id}/stream-url
    → Instant playback: default TTS audio (pre-generated)
    → Optional: Switch to cloned voice (generates on first use, then cached)
    → Optional: Enable avatar (uploads image if none; generates talking video async)
    → Save position; add to favorites
    → Share: deep link OR Share Clip (Premium+) — create 15–60s video for Instagram/YouTube
```

### 1.4 Final Admin/Content Manager Flow

```
Admin dashboard → Stories
    → Create: title, content, theme, age, language (ta), emotion_mode
    → Save as DRAFT
    → Trigger pipeline (translate, TTS)
    → Story for review: listen to narration, approve or reject
    → On approve: narration_approved_at set; story live
    → Bulk: publish, category, retry failed
```

### 1.5 Final Personalization Flow

```
Default: Pre-generated neural TTS (Google Cloud) → instant
Cloned voice: Upload/record → Voice cloning job (ElevenLabs/XTTS)
    → First play: on-demand generation → cache
    → Subsequent: cached audio
Avatar: Upload image → parent_avatar stored
    → First play with avatar: trigger talking-video job
    → Fallback: static avatar + audio while generating
    → Ready: return avatarVideoUrl
```

### 1.6 Final Avatar/Talking Video Flow

```
Parent has avatar + plays story with voice profile
    → AvatarVideoService.getAvatarVideoUrl(storyId, source, parentId, lang, voice)
    → Cache lookup: story_avatar_video (story_id, story_source, parent_id, lang, voice)
    → READY: return signed URL
    → MISSING: create PENDING, trigger async (HeyGen/Replicate/D-ID)
    → Avatar image + narration audio URL → external API → poll → S3 upload → READY
    → Client: static avatar + audio first; next request gets video URL
```

### 1.6.1 Share Clip Flow (MVP — Premium+)

```
Premium+ user taps "Share Clip" in player
    → Select segment: 15–60 seconds (slider or "Share last 30s")
    → POST /api/v1/stories/{id}/share-clip { startSeconds, durationSeconds, format }
    → Backend: check entitlement → create shareable_clips row (PENDING)
    → Trigger async SHARE_CLIP job
    → Return jobId; client polls GET /api/v1/share-clips/{jobId}
    → Job: extract audio segment → get/create avatar video segment → composite → add "Made with Tamixa" watermark
    → Store in S3 → shareable_clips.status = READY, storage_path set
    → Client: GET download URL → save to device → open system share (Instagram, YouTube, etc.)
    → Limit: 5–10 clips/month per Premium+ (configurable)
```

---

## 1.7 System Modules (Complete)

| # | Module | MVP Status | Location |
|---|--------|------------|----------|
| 1 | Story content pipeline | ✅ Existing | StoryProcessingService, TranslationService, RewriteService |
| 2 | Review and approval workflow | ✅ Existing | Admin approve-narration, narration_approved_at |
| 3 | Story library service | ✅ Existing | StoryLibraryService, LibraryStoryController |
| 4 | Story recommendation service | ✅ Existing | StoryRecommendationService |
| 5 | TTS generation service | ✅ Existing | TTSService, StoryProcessingService |
| 6 | Voice cloning service | ✅ Existing | VoiceCloningService, ElevenLabs/XTTS |
| 7 | Avatar upload and management | ✅ Existing | FamilyAvatarService |
| 8 | Lip-sync / talking video service | ✅ Existing | AvatarVideoService, HeyGen/Replicate/D-ID |
| 9 | Story playback service | ✅ Existing | AudioStreamService, StreamUrlController |
| 10 | Story continuation service | ✅ Existing | PlaybackPositionService, getRecent |
| 11 | Share clip generation | ✅ **MVP: Premium+** | ShareClipService — request clip → async render → download/share URL |
| 12 | Subscription / premium gating | ✅ Existing | SubscriptionService, SubscriptionGuard |
| 13 | Analytics events | ✅ Existing | StoryAnalyticsController, story_analytics |

---

## 1.8 Exact MVP Use Cases

| # | Use Case | Actor | Flow |
|---|----------|-------|------|
| 1 | Browse story library | Parent | Open app → Dashboard → See library by category/recommendation → Tap story |
| 2 | Play approved story instantly | Parent | Tap story → GET stream-url → Pre-generated audio plays immediately |
| 3 | Continue previous story | Parent | Dashboard "Continue Adventure" → Tap → Player resumes at saved position |
| 4 | Listen with default voice | Parent | stream-url without voiceProfile → Default TTS audio |
| 5 | Listen with cloned voice | Parent | Upload voice → Clone created → Select "My voice" → On-demand gen then cached |
| 6 | Upload avatar and generate talking video | Parent | Upload image → Play story with avatar → Async job → Video ready on next play |
| 7 | Save story to favorites/history | Parent | Tap favorite on card/player → Saved; playback position auto-saved |
| 8 | Share story clip | Parent (Premium+) | Select 15–60s segment → Request clip → Async render → Download/share to Instagram/YouTube |
| 9 | Admin reviews and approves stories | Admin | Story for review → Listen → Approve → Story goes live |

---

## 2. Final Architecture Diagram (Text)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              TAMIXA MVP ARCHITECTURE                              │
└─────────────────────────────────────────────────────────────────────────────────┘

┌──────────────┐     ┌──────────────┐
│ Mobile (KMP) │     │ Admin (Next) │
│ Compose      │     │ React        │
└──────┬───────┘     └──────┬───────┘
       │                    │
       │  HTTPS / REST      │  HTTPS / REST
       ▼                    ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         API GATEWAY / SPRING BOOT                                 │
│  /api/v1/auth | stories | playback | voice | admin | health                       │
└─────────────────────────────────────────────────────────────────────────────────┘
       │
       ├──► StoryLibraryService ──► library_stories, story_translations
       ├──► AudioStreamService ──► story_narration_audio, S3
       ├──► AvatarVideoService ──► story_avatar_video, HeyGen/Replicate
       ├──► VoiceCloningService ──► voice_profiles, voice_cloning_jobs
       ├──► PlaybackPositionService ──► story_playback_position
       ├──► StoryRecommendationService ──► favorites, library
       ├──► StoryProcessingService ──► Pipeline (translate→rewrite→TTS)
       ├──► ShareClipService ──► shareable_clips, clip render (avatar+audio)
       └──► SubscriptionService ──► subscription, entitlements
       │
       ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ PostgreSQL   │  │ Redis        │  │ S3           │  │ External     │
│ (primary)    │  │ (cache)      │  │ (media)      │  │ APIs         │
│              │  │              │  │              │  │ Google TTS   │
│ parents      │  │ TTS metadata │  │ audio        │  │ OpenAI       │
│ library_*    │  │ translation  │  │ avatars      │  │ ElevenLabs   │
│ stories      │  │              │  │ videos      │  │ HeyGen/D-ID  │
│ voice_*       │  │              │  │ covers      │  │ Replicate   │
│ playback_*   │  │              │  │ share_clips  │  │              │
│ shareable_   │  │              │  │              │  │              │
│ clips        │  │              │  │              │  │              │
└──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘

Background (thread pools, @Async):
  - StoryProcessingService.processLanguagesInternal (translate, TTS)
  - AvatarVideoService (trigger async generation)
  - ShareClipService (clip render: extract segment → composite → watermark)
  - VoiceCloningService (ElevenLabs addVoice)
  - RetryFailedStoriesJob (@Scheduled)
  - DataExportProcessor, SubscriptionScheduledJobs
```

---

## 3. Database Schema (Final)

Existing tables are retained. Key tables and augmentations:

### 3.1 Core Tables (Existing – Use As-Is)

| Table | Key Fields | Relationships | Indexes |
|-------|------------|---------------|---------|
| `parents` | id, email, password_hash, role | — | idx_parents_email |
| `children` | id, parent_id, name, language_preference | parents | idx_children_parent_id |
| `library_stories` | id, title, content, theme, language, age, status, narration_approved_at, cover_image_url, emotion_mode | — | language, age, status, emotion_mode |
| `story_translations` | id, master_story_id, language, content, status, retry_count | library_stories | (master_story_id, language) UNIQUE |
| `story_narration_scripts` | id, translation_id, script_text, tone_mode | story_translations | translation_id UNIQUE |
| `story_narration_audio` | id, translation_id, voice_profile, audio_url, duration_seconds, status | story_translations | (translation_id, voice_profile) UNIQUE |
| `story_playback_position` | id, parent_id, story_id, story_source, position_seconds | parents, (story_id, story_source) | parent_id, updated_at |
| `voice_profiles` | id, parent_id, elevenlabs_voice_id, reference_audio_path | parents | parent_id |
| `voice_cloning_jobs` | id, voice_profile_id, status | voice_profiles | status |
| `parent_avatar` | id, parent_id, storage_path | parents | parent_id UNIQUE |
| `story_avatar_video` | id, story_id, story_source, parent_id, language, voice_profile, storage_path, status | parents | (story_id, story_source, parent_id, lang, voice) UNIQUE |
| `subscription` | id, parent_id, status, tier | parents | parent_id |
| `favorite_story` | id, parent_id, story_id, story_source | parents | (parent_id, story_id, story_source) |
| `processing_job` | id, job_type, resource_type, resource_id, status, progress | — | resource, status |

### 3.2 Schema Additions for MVP

| Table | Purpose | Key Fields | Indexes |
|-------|---------|------------|---------|
| `shareable_clips` | Premium+ share clip generation | id, parent_id, story_id, story_source, language, voice_profile, start_seconds, duration_seconds, format (9:16, 1:1), storage_path, status, job_id, created_at | (parent_id, created_at), status |
| `story_categories` (optional) | Normalize categories | id, name, slug | — |
| `story_library_category` (optional) | Many-to-many library ↔ category | story_id, category_id | — |

**shareable_clips lifecycle:** PENDING → PROCESSING → READY / FAILED. Limit clips per parent per month (enforced in service). Format: `9:16` (IG Reels, TikTok), `1:1` (IG feed).

### 3.3 Lifecycle Notes

- **library_stories**: DRAFT → PROCESSING → PUBLISHED/READY. Only READY + narration_approved_at visible in app.
- **story_translations**: PENDING → TRANSLATING → REWRITING → TTS_PROCESSING → COMPLETED (or *_FAILED).
- **story_avatar_video**: PENDING → PROCESSING → READY / FAILED.
- **voice_cloning_jobs**: PENDING → PROCESSING → COMPLETED / FAILED.

---

## 4. API Design (Final)

### 4.1 Auth

| Method | Path | Purpose |
|--------|------|---------|
| POST | /api/v1/auth/register | Register (email, password, terms) |
| POST | /api/v1/auth/login | Login → JWT |
| POST | /api/v1/auth/refresh | Refresh token |
| GET | /api/v1/auth/me | Current user |

### 4.2 Story Listing & Details

| Method | Path | Purpose |
|--------|------|---------|
| GET | /api/v1/stories/library | List approved library stories (language, page, size) |
| GET | /api/v1/stories/library/{id} | Story detail (approved only) |
| GET | /api/v1/stories/recommended | Recommended (language, childId?, limit) |
| GET | /api/v1/stories | My generated stories (parent) |

### 4.3 Playback

| Method | Path | Purpose |
|--------|------|---------|
| GET | /api/v1/stories/library/{id}/stream-url | Audio + avatar + avatarVideo URL (language, voiceProfile) |
| GET | /api/v1/stories/{id}/stream-url | Unified: library first, then generated |
| GET | /api/v1/stories/{id}/narration-script | Script for TTS fallback |
| GET | /api/v1/stories/{id}/voices | Available voices (default, calm, cloned:id) |
| GET | /api/v1/playback/position | Get position (storyId, storySource) |
| POST | /api/v1/playback/position | Save position |
| GET | /api/v1/playback/recent | Continue listening (recent with positions) |

### 4.4 Voice & Avatar

| Method | Path | Purpose |
|--------|------|---------|
| POST | /api/v1/voice/upload | Upload voice sample → clone |
| GET | /api/v1/voice/profiles | List voice profiles |
| PUT | /api/v1/stories/{id}/voice-preference | Save voice preference |
| POST | /api/v1/parents/me/avatar | Upload avatar image |
| DELETE | /api/v1/parents/me/avatar | Remove avatar |
| POST | /api/v1/stories/{id}/upload-family-voice | Record family voice for story |
| DELETE | /api/v1/stories/{id}/delete-family-voice | Delete family voice |

### 4.5 Share (MVP)

| Method | Path | Purpose |
|--------|------|---------|
| GET | /api/v1/stories/library/{id}/share-url | Return shareable deep link (e.g. https://tamixa.com/s/{id}) |
| POST | /api/v1/stories/{id}/share-clip | **Premium+** Request clip generation. Body: `{ startSeconds, durationSeconds, format? }`. Returns `{ clipId, jobId, status }` |
| GET | /api/v1/share-clips/{clipId} | Get clip status + download URL when READY |
| GET | /api/v1/share-clips/quota | **Premium+** Remaining clips this month (e.g. `{ remaining: 7, limit: 10 }`) |

**Share Clip flow:** User selects segment → POST → async job → poll GET until READY → download URL → save/share to Instagram/YouTube.

### 4.6 Admin

| Method | Path | Purpose |
|--------|------|---------|
| GET/POST | /api/v1/admin/stories | CRUD library stories |
| POST | /api/v1/admin/stories/{id}/trigger-pipeline | Start TTS pipeline |
| POST | /api/v1/admin/stories/{id}/approve-narration | Set narration_approved_at |
| POST | /api/v1/admin/stories/{id}/reject-narration | Reject and flag |
| GET | /api/v1/admin/stories/for-review | Stories pending review |
| GET | /api/v1/admin/processing-jobs | Job status |
| GET | /api/v1/admin/health | Health |

---

## 5. Background Job Design

### 5.1 Job Types

| Job Type | Resource | Trigger | Steps |
|----------|----------|---------|-------|
| STORY_PIPELINE | curated_story / library_story | Admin trigger | Translate → Rewrite → TTS per language |
| VOICE_CLONE | voice_profile | Voice upload | ElevenLabs addVoice or XTTS |
| AVATAR_VIDEO | story_avatar_video | First playback with avatar | HeyGen/Replicate create → poll → S3 |
| **SHARE_CLIP** | shareable_clips | POST /share-clip (Premium+) | Extract audio segment → get/create avatar segment → FFmpeg composite → watermark → S3 |

### 5.2 Job States

- PENDING, IN_PROGRESS, COMPLETED, FAILED

### 5.3 Retries

- Story pipeline: max 3 retries per language (exponential backoff 2s, 5s, 10s)
- Voice clone: provider-dependent (ElevenLabs usually synchronous)
- Avatar video: 1 retry on transient failure
- Share clip: 1 retry on FFmpeg or storage failure

### 5.4 Failure Handling

- Mark status FAILED; store error_message in processing_job
- Admin can retry via "Retry failed" in Story for review
- Scheduled RetryFailedStoriesJob (every 30 min) for TRANSLATION_FAILED, TTS_FAILED

### 5.5 Fallbacks

- **Audio:** Narration script available for device TTS fallback
- **Avatar:** Static image + audio while video generates
- **Cloned voice:** Fall back to default TTS if clone not ready

---

## 6. Story Asset Strategy (Fast Playback)

| Asset | Strategy |
|-------|----------|
| **Audio** | Pre-generated at publish. One file per (translation, voice_profile). Stored in S3. Presigned URLs or CDN. |
| **Cover** | Pre-generated (AI or upload). S3 + CDN. |
| **Narration script** | Cached in story_narration_scripts. Fallback for device TTS. |
| **Timeline JSON** | MVP: not exposed. Single audio stream. Phase 2: segments with timestamps for subtitles. |
| **Subtitles** | Phase 2. MVP: none. |
| **Lip-sync metadata** | Internal to avatar video. No separate API. |
| **Avatar video** | On-demand generation, then cached. S3 presigned. |
| **Share clips** | On-demand: extract audio + avatar segment → FFmpeg composite → watermark → S3. Cached per (story, parent, start, duration, format). |
| **CDN** | Use S3 public URLs or CloudFront when configured. |

**Design:** Approved stories have audio before they appear in the library. Playback = one GET stream-url → presigned audio URL → play. Share clips are generated async; Premium+ only.

---

## 7. Playback Model (Story Timeline)

**MVP:** Single continuous audio stream. No timeline API.

**Phase 2 timeline model:**

```json
{
  "storyId": 1,
  "language": "ta",
  "durationSeconds": 180,
  "segments": [
    {
      "index": 0,
      "startMs": 0,
      "endMs": 5000,
      "type": "narrator",
      "text": "Once upon a time..."
    },
    {
      "index": 1,
      "startMs": 5000,
      "endMs": 8000,
      "type": "dialogue",
      "character": "rabbit",
      "text": "Hello, friend!"
    }
  ]
}
```

**MVP implementation:** Not built. App consumes raw audio URL + narration script (fallback). Timeline in Phase 2 for subtitles and interactive storytelling.

---

## 8. Recommendation & Home Screen Data Model

### 8.1 Home Screen Sections

| Section | Source | Backend |
|---------|--------|---------|
| **Continue Adventure** | story_playback_position (recent, position > 0) | GET /playback/recent |
| **Recommended** | Favorites + interests + age | GET /stories/recommended |
| **Popular** | Play count or manual curation | Same as library, sorted by play count (Phase 2) |
| **Categories** | library_stories.theme distinct | GET /stories/library?category=X or group by theme |
| **Family Voice Stories** | Stories with family voice or cloned | Filter recommended/stories |
| **Tell me a story** | Optional entry | Navigate to StoryGeneration |

### 8.2 Backend Response Contracts

**Continue Adventure (GET /playback/recent):**
```json
[
  {
    "storyId": 1,
    "storySource": "library",
    "positionSeconds": 120,
    "updatedAt": "2025-03-14T10:00:00Z",
    "title": "...",
    "coverImageUrl": "...",
    "progress": 0.4
  }
]
```

**Recommended (GET /stories/recommended):**
```json
[
  {
    "storyId": 1,
    "storySource": "library",
    "title": "...",
    "theme": "...",
    "age": 5,
    "reason": "In your favorites"
  }
]
```

**Enhancement:** Extend PlaybackPositionService/controller to join with library_stories for title, cover, and progress. Add `/api/v1/home` aggregator in Phase 2.

---

## 9. MVP Scope: 30-Day Launch

### 9.1 Must Build Now

- [ ] **Share clip generation (Premium+)** — User selects 15–60s segment → async render → download/share to Instagram/YouTube. Formats: 9:16 (Reels), 1:1 (IG feed). Limit 5–10 clips/month.
- [ ] Home screen: Continue Adventure (from playback/recent, enrich with story meta)
- [ ] Home screen: Recommended (existing)
- [ ] Home screen: Categories (group by theme; filter library)
- [ ] Library-first navigation: default tab = library, not generated
- [ ] Share: deep link `https://tamixa.com/s/{id}` (web page or app open)
- [ ] Story continuation: prominent "Continue Listening" from recent
- [ ] De-emphasize "Tell me a story": move to secondary CTA or settings
- [ ] Admin: ensure approval workflow is clear and fast
- [ ] Performance: ensure stream-url responds < 200ms
- [ ] Subscription gating: avatar video, cloned voice, **share clip** behind Premium+ paywall

### 9.2 Nice to Have (If Time)

- [ ] Popular stories (play count aggregation)
- [ ] Family Voice Stories section
- [ ] Share clip: optional 16:9 format for YouTube

### 9.3 Later Phase

- [ ] Timeline JSON + subtitles
- [ ] Interactive storytelling (choose-your-path)
- [ ] AI story generation polish
- [ ] story_categories table + admin category management

---

## 10. Implementation Roadmap (30 Days)

### Week 1: Core UX & Data

| Day | Task |
|-----|------|
| 1–2 | Home API: extend `/playback/recent` to return story title, cover, progress; add optional `/home` aggregator |
| 3 | Mobile: Dashboard defaults to library; "Continue Adventure" from recent with progress bar |
| 4 | Mobile: Categories filter (theme-based) |
| 5 | Share: backend `/stories/library/{id}/share-url`; mobile/web deep link handling |

### Week 2: Content & Share Clip Backend

| Day | Task |
|-----|------|
| 6 | Share clip: Flyway migration for `shareable_clips` table |
| 7 | Share clip: ShareClipService — request, quota check, job trigger; FFmpeg/adapter for segment extract + composite |
| 8 | Share clip: async job (extract audio segment, reuse avatar video or generate short clip, watermark, S3) |
| 9 | Share clip: GET clip status, download URL; Premium+ gating |
| 10 | Admin: streamline approval; Pipeline retry; performance (stream-url, Redis) |

### Week 3: Mobile Share Clip & Gating

| Day | Task |
|-----|------|
| 11 | Mobile: Share Clip UI — segment picker (15–60s), request, polling, download |
| 12 | Mobile: Wire share to system share sheet with clip file; Premium+ upsell when not entitled |
| 13 | Subscription: enforce avatar video, cloned voice, share clip gating |
| 14 | Analytics: playback, favorites, share, **share_clip_requested**, **share_clip_downloaded** |
| 15 | Documentation: API docs, runbook |

### Week 4: Launch Prep

| Day | Task |
|-----|------|
| 16–17 | Security review: no PII in logs, rate limits |
| 18 | Load test: stream-url, library list |
| 19–20 | Bug fixing, UX polish |
| 21 | Go/no-go; deploy |

### Step-by-Step Build Roadmap (Implementation Order)

1. **Backend: Home/Continue API** – Extend `PlaybackPositionService.getRecentWithPositions` to join `library_stories` for title, cover, progress.
2. **Backend: Share URL** – Add `GET /stories/library/{id}/share-url` returning deep link.
3. **Backend: Share Clip** – `shareable_clips` table, ShareClipService, POST /share-clip, GET /share-clips/{id}, quota. Async job: extract segment → composite → watermark → S3.
4. **Mobile: Dashboard** – Load library first; Continue Adventure at top; Categories filter.
5. **Mobile: Share** – Deep link + **Share Clip** (Premium+): segment picker → request → poll → download → system share to Instagram/YouTube.
6. **Admin: Approval UX** – One-click approve; bulk approve.
7. **Subscription: Gating** – Avatar video, cloned voice, **share clip** (Premium+).
8. **Performance** – Redis cache for stream-url; DB indexes.
9. **Docs & Runbook** – .env.example, API docs, deployment.
10. **Launch** – Deploy, monitor, iterate.

---

## 11. Folder Structure

```
backend/src/main/kotlin/com/tamixa/
├── api/
│   ├── auth/
│   ├── library/
│   ├── stream/
│   ├── playback/
│   ├── voice/
│   ├── recommendation/
│   ├── controller/   # Admin, Health
│   └── ...
├── application/
│   ├── storylibrary/
│   ├── narration/
│   ├── stream/
│   ├── avatar/
│   ├── voice/
│   ├── playback/
│   ├── recommendation/
│   └── ...
├── domain/
├── infrastructure/
└── ...

mobile/
├── composeApp/
│   └── src/commonMain/kotlin/com/tamixa/
│       ├── ui/screen/
│       ├── network/
│       ├── domain/
│       └── ...
├── androidApp/
└── iosApp/

admin/
└── src/app/(dashboard)/dashboard/
```

---

## 12. Key Tradeoffs & CTO Recommendation

| Tradeoff | Decision | Rationale |
|----------|----------|------------|
| Improve vs replace | **Improve** | Existing pipeline works; replacing adds risk. |
| Flutter vs KMP | **KMP** | Already invested; both platforms supported. |
| Timeline API in MVP | **No** | Single audio sufficient; timeline for Phase 2 subtitles. |
| **Share clip generation** | **IN MVP (Premium+)** | Viral growth driver; Instagram/YouTube-ready clips attract millions. Worth the engineering investment. |
| story_categories table | **Defer** | Use theme as category for MVP. |
| Separate job queue (Redis/Bull) | **Defer** | Thread pools + @Async sufficient for MVP scale. |

**Final CTO recommendation:** Improve the current architecture. Focus the team on: (1) **Share clip (Premium+)** as top growth feature, (2) Library-first UX, (3) Continue Adventure prominence, (4) Share deep link, (5) Admin approval speed, (6) Subscription gating.

---

## 13. Competitor Analysis

| Competitor | Strength | Gap Tamixa Fills |
|------------|----------|------------------|
| **Epic!** | Large curated library | Tamixa: personalized voice + avatar |
| **Audible Kids** | Big catalog | Tamixa: family voice, Tamil-first |
| **Storyline Online** | Celeb narrators | Tamixa: cloned grandparent voice |
| **Calm Kids** | Sleep stories | Tamixa: interactive avatar, continuation |

**Differentiator:** Talking avatar + cloned family voice for emotional connection. **Share Clip (Instagram/YouTube-ready video)** drives viral growth—parents share to millions. Library-first ensures quality; personalization enhances without blocking.

---

## Appendix A: Service Interfaces Summary

| Service | Key Methods |
|---------|-------------|
| StoryLibraryService | findByLanguageApprovedOnly, findByIdAndLanguage |
| AudioStreamService | getLibraryStreamUrl, getLibraryNarrationStreamUrl, getFamilyVoiceStreamUrl |
| AvatarVideoService | getAvatarVideoUrl (triggers async if missing) |
| **ShareClipService** | **requestClip, getClipStatus, getDownloadUrl, getQuotaRemaining** |
| VoiceCloningService | cloneFromUpload, getOrCreateProfile |
| PlaybackPositionService | savePosition, getPosition, getRecentWithPositions |
| StoryRecommendationService | getRecommended |
| StoryProcessingService | processSync, retryFailed |
| SubscriptionService | getOrCreateSubscription, isEntitledToUnlimitedStories, isEntitledToShareClips |

---

## Appendix B: Mobile Screen Flow

```
Splash → Login/Register → Language Selection → Dashboard
    ├── Continue Adventure (tap) → Audio Player
    ├── Recommended (tap) → Audio Player
    ├── Categories (tap) → Category list → Story list → Audio Player
    ├── Tell me a story (secondary) → Story Generation
    ├── My Voice & Avatar → Voice Upload / Avatar Upload
    └── Settings

Audio Player:
    ├── Play/pause, progress, voice picker
    ├── Avatar (if enabled): static or video
    ├── Favorite, Share (deep link + Share Clip for Premium+), Sleep timer
    └── Back → Dashboard (position saved)

Share Clip (Premium+):
    Tap Share → Option: "Share link" or "Create clip for Instagram/YouTube"
    → Segment picker (15–60s) → Request → Poll status → Download → System share
```
