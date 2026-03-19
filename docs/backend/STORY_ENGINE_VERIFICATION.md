# Story Engine Verification Report

**Date:** 2025-03-14  
**Status:** Gap analysis vs ideal Tamixa story engine

---

## Ideal Story Engine Process

The Tamixa story engine should convert raw stories into **structured assets**—not just store paragraphs.

### Stage 1: Normalize Story

Convert raw story into:

| Field    | Purpose                          |
|----------|----------------------------------|
| title    | Story title                      |
| category | Browse/categorize (e.g. Adventure)|
| theme    | Story theme / setting            |
| moral    | Lesson or takeaway               |
| scenes   | **Structured scenes** (not flat text) |

### Stage 2: Segment Story

Each scene becomes:

- **Narration segments** — narrator voice
- **Optional dialogue segments** — character speech

### Stage 3: Prepare Playback Metadata

For each segment:

| Field           | Purpose                    |
|-----------------|----------------------------|
| speaker         | "Narrator" or character    |
| text            | Segment text               |
| subtitle        | Display text (may differ)  |
| audio key       | Per-segment audio URL/key  |
| estimated duration | durationMs              |

### Stage 4: Generate Timeline

Target format (what the app plays):

```json
{
  "storyId": "story_101",
  "title": "Murugan and the Lantern",
  "scenes": [
    {
      "sceneId": "scene_1",
      "backgroundHint": "forest_evening",
      "segments": [
        {
          "segmentId": "seg_1",
          "speaker": "Narrator",
          "text": "Murugan walked slowly through the forest.",
          "audioUrl": "https://cdn/.../seg_1.mp3",
          "durationMs": 2400
        }
      ]
    }
  ]
}
```

---

## Current vs Ideal Mapping

### Stage 1: Normalize Story

| Ideal       | Current State                                      | Gap |
|-------------|----------------------------------------------------|-----|
| title       | ✅ `LibraryStory.title`, `StoryTranslation.title`   | —   |
| category    | ⚠️ `theme` used as category (no separate field)    | Minor: theme serves dual purpose |
| theme       | ✅ `LibraryStory.theme`                            | —   |
| moral       | ✅ `LibraryStory.moral`, `StoryTranslation.moral`  | —   |
| scenes      | ❌ Raw `content` only; no structured scenes       | **Missing** |

**Gaps:**

- **No scene extraction**: Content is stored as flat text. There is no step that parses raw story into discrete scenes with boundaries.
- **No background hints**: `story_scenes.background_hint` exists but is always `null`.
- **Generated stories**: `StoryPromptBuilder` returns JSON with `category`, `theme`, `story_text`—we map to `theme` (category is folded in).

---

### Stage 2: Segment Story

| Ideal                 | Current State                                      | Gap |
|------------------------|----------------------------------------------------|-----|
| Narration segments     | ✅ Paragraphs → segments, `speaker="Narrator"`     | —   |
| Dialogue segments      | ⚠️ `EmotionTaggingService` detects DIALOGUE        | **Not persisted** |

**Current flow:**

1. `EmotionTaggingService.tagEmotions()` produces `EmotionSegment` with `EmotionTag.DIALOGUE`, `CONVERSATIONAL`, etc.
2. SSML builder uses these for prosody (pitch, rate) — **in-memory only**.
3. `StorySceneRepositoryAdapter.saveSceneWithSegments()` splits by `\n\n`; all segments get `speaker = "Narrator"`.
4. **Emotion/dialogue labels are never stored** in `story_segments`.

**Gaps:**

- Dialogue segments are not distinguished from narration in the DB.
- No `speaker` variety (e.g. character names for dialogue).

---

### Stage 3: Prepare Playback Metadata

| Ideal Field        | Current State                              | Gap |
|--------------------|--------------------------------------------|-----|
| speaker            | ✅ `story_segments.speaker` (always Narrator) | Limited |
| text               | ✅ `story_segments.text`                    | —   |
| subtitle           | ⚠️ Same as `text`                          | No separate field; acceptable for MVP |
| audio key          | ⚠️ Single audio for whole story            | **Per-segment audio missing** |
| estimated duration  | ✅ `story_segments.duration_ms` (proportional) | Approximate only |

**Gaps:**

- **Single TTS file per story/language**: One `story_narration_audio` row; `audioUrl` shared. Ideal has `seg_1.mp3`, `seg_2.mp3` per segment.
- **Duration**: Proportional split by paragraph count, not actual TTS timing.
- **Subtitle**: `text` serves as subtitle; no separate `subtitle` field (fine for MVP).

---

### Stage 4: Generate Timeline

| Ideal Field      | Current State                          | Gap |
|------------------|----------------------------------------|-----|
| storyId           | ✅ `PlaybackManifest.storyId` (Long)    | Minor: spec shows string |
| title             | ✅                                     | —   |
| scenes           | ✅ `PlaybackScene` list                 | Only 1 scene today |
| sceneId           | ✅ "scene_1"                           | —   |
| backgroundHint    | ❌ Always `null`                       | **Never populated** |
| segmentId         | ✅ "seg_1", "seg_2"                    | —   |
| speaker           | ✅                                     | —   |
| text              | ✅                                     | —   |
| audioUrl          | ⚠️ Same URL for all segments            | Per-segment URL ideal |
| durationMs        | ✅                                     | —   |

**API:** `GET /api/v1/stories/{id}/timeline` → `PlaybackManifest` — ✅ Present.

---

## Summary: Gaps and Priorities

| Priority | Gap                         | Effort  | Recommendation |
|----------|-----------------------------|---------|-----------------|
| **P0**   | Per-segment audio (seg_1.mp3, etc.) | High   | Infrastructure ready; config `per-segment-tts`; pipeline path to be wired |
| **P1**   | Scene extraction + backgroundHint  | Medium | Implemented: StorySceneExtractor, theme→backgroundHint |
| **P1**   | Narration vs dialogue in segments   | Medium | Implemented: segment_type DIALOGUE/NARRATION, speaker "Character" |
| **P2**   | Multiple scenes per story           | Medium | Implemented: multiple story_scenes, findScenesByTranslationId |
| **P2**   | Accurate per-segment duration        | Low    | Word-proportional; exact when per-segment TTS enabled |
| **P3**   | storyId as string in API             | Low    | Implemented: "story_123" format |

---

## Package Layout (Point 8)

Current layout aligns with `docs/backend/MODULAR_MONOLITH_STRUCTURE.md`:

```
com.tamixa
├── api              # Controllers, DTOs (stream, playback)
├── application      # Business logic
│   ├── narration    # TTS pipeline, emotion tagging, SSML
│   ├── playback     # PlaybackManifestService
│   ├── storylibrary # StoryLibraryService
│   └── ...
├── domain           # LibraryStory, StoryScene, StorySegment, EmotionTag
└── infrastructure   # persistence, narration adapters
```

**Recommended additions** if story engine is expanded:

| New Component          | Package                           | Purpose |
|------------------------|-----------------------------------|---------|
| StoryNormalizationService | `application.storyengine`      | Stage 1: raw → title, scenes, theme, moral |
| StorySegmentationService  | `application.storyengine`      | Stage 2: scenes → narration + dialogue segments |
| PlaybackMetadataService   | `application.playback` (extend) | Stage 3: segments → speaker, audio key, duration |
| (existing) PlaybackManifestService | `application.playback`     | Stage 4: manifest/timeline generation |

---

## Recommendations

1. **Short term (MVP)**: Current design is acceptable. Single audio + proportional duration works for basic playback.
2. **Medium term**: Add scene extraction (Stage 1) and populate `background_hint` from theme/summary.
3. **Long term**: Per-segment TTS for accurate sync; persist narration vs dialogue from emotion tagging.
