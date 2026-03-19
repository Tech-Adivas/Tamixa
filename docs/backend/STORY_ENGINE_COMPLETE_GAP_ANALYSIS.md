# Story Engine — Complete Gap Analysis

**Date:** 2025-03-14  
**Scope:** First-time, re-run, library vs generated, edge cases, package layout

---

## 1. Workflow Overview

| Flow | Trigger | Pipeline | Structured Assets | Timeline |
|------|---------|----------|-------------------|----------|
| **Library first-time** | Approve narration | StoryProcessingService | ✅ scenes, segments | ✅ from DB or runtime split |
| **Library re-publish** | Republish languages | StoryProcessingService | ✅ replace scenes | ✅ |
| **Library content edit** | Invalidate & reprocess | StoryProcessingService | ✅ (deletes translations → cascades scenes) | ✅ |
| **Generated first-time** | Story created | GeneratedStoryNarrationService | ❌ none | Runtime split only |
| **Generated re-run** | N/A (no republish) | — | — | — |

---

## 2. Stage-by-Stage Gaps

### Stage 1: Normalize Story

| Ideal | Library | Generated | Gap |
|-------|---------|-----------|-----|
| title | ✅ | ✅ | — |
| category | ⚠️ theme as category | ⚠️ theme | No separate `category` column |
| theme | ✅ | ✅ | — |
| moral | ✅ | ✅ | — |
| scenes | ⚠️ From markers only | ❌ Never extracted | See below |

**Scene extraction (library only):**
- ✅ `StorySceneExtractor` splits by `---` / `***`
- ❌ No markers in typical author content → effectively 1 scene
- ❌ No LLM-based scene detection (e.g. “Scene 2: At the village”)

**Recommendation:** Optional LLM step to insert scene markers or return structured `{scenes: [...]}` from rewrite.

---

### Stage 2: Segment Story

| Ideal | Library | Generated | Gap |
|-------|---------|-----------|-----|
| Narration segments | ✅ | ✅ (runtime) | — |
| Dialogue segments | ✅ segment_type | ❌ | Generated never persists |

**Library:** Emotion tagging → DIALOGUE/NARRATION persisted in `story_segments.segment_type`.  
**Generated:** No `story_scenes`/`story_segments`; always runtime paragraph split.

---

### Stage 3: Prepare Playback Metadata

| Field | Library | Generated | Gap |
|-------|---------|-----------|-----|
| speaker | ✅ Narrator/Character | ✅ Narrator (runtime) | — |
| text | ✅ | ✅ | — |
| subtitle | ⚠️ = text | ⚠️ = text | No separate subtitle field |
| audio key | ⚠️ Single per story | ⚠️ Single | Per-segment audio not wired |
| duration | ⚠️ Proportional | ⚠️ Proportional | Not from actual TTS |

---

### Stage 4: Generate Timeline

| Field | Library | Generated | Gap |
|-------|---------|-----------|-----|
| storyId | ✅ "story_123" | ✅ | — |
| title | ✅ | ✅ | — |
| scenes | ✅ Multiple when markers present | ✅ 1 (runtime) | — |
| backgroundHint | ✅ From theme | ❌ null | Generated never sets |
| segmentId | ✅ | ✅ | — |
| audioUrl | ⚠️ Same for all segments | ⚠️ Same | Per-segment when P0 done |

---

## 3. First-Time vs Re-Run Flows

### Library — First Time

1. Create story (DRAFT) → `library_stories` + event
2. Submit for review → PUBLISHED, `resetPipelineStateForSubmitForReview` deletes translations (→ cascades scripts, scenes)
3. Approve narration → triggers pipeline
4. Pipeline: translate → rewrite → TTS → `saveSceneWithSegments`
5. Timeline: `findScenesByTranslationId` → manifest

**Gaps:** None for happy path.

### Library — Re-Publish (selected languages)

1. Admin republish → `invalidateNarrationAudioForLanguages(languages)`
2. Deletes: narration_audio, story_scenes (per translation)
3. Keeps: translations, narration_scripts
4. Pipeline runs for selected languages
5. `saveSceneWithSegments` replaces scenes for those languages

**Gaps:** ✅ Scenes correctly replaced. ✅ Idempotent.

### Library — Content Edit (invalidate & reprocess)

1. Admin edits content → `invalidateAndReprocess`
2. `invalidateContent`: deletes all translations (→ cascades scripts, scenes)
3. Pipeline runs fresh (translate, rewrite, TTS)
4. New translations + scenes created

**Gaps:** ✅ Full reset. StoryVersion created on `repository.update()` (master content edit).

### Library — Update Translation Content Only

`updateTranslationContent` updates translation row; does NOT:
- Delete narration_audio
- Delete story_scenes
- Trigger pipeline

**Gap:** Stale scenes if admin edits translation content without republish. Scenes/audio stay from previous pipeline run.

### Generated — First Time

1. Story created → InlineStoryEventPublisher → GeneratedStoryNarrationService
2. Rewrite → TTS → upload single audio
3. No scenes/segments persisted
4. Timeline: runtime `splitScriptIntoSegments`

**Gap:** Generated stories never get structured assets (scenes, segments, dialogue). Timeline always runtime-only.

---

## 4. Edge Cases & Robustness

| Case | Behavior | Gap |
|------|----------|-----|
| Emotion tagging fails validation | Fallback to plain SSML; segments all NARRATION | ✅ Acceptable |
| Scene extraction: no markers | 1 scene, full script | ✅ OK |
| Scene extraction: multiple chunks, one empty | Filtered by `.filter { it.isNotBlank() }` | ✅ |
| Partial pipeline failure (1 lang fails) | Other langs complete; story → PUBLISHED | ✅ |
| Republish while pipeline running | `progressTracker`; invalidation clears | ⚠️ Race possible |
| StoryVersion on every update | ✅ Created in `StoryLibraryRepositoryAdapter.update` | — |
| StoryVersion on updateTranslationContent | Only when source lang + `repository.update` | ⚠️ Translation-only edit may not create version |

---

## 5. Missing / Improvements

### High Priority

1. **Generated stories — structured assets**
   - No `story_translations` for generated; no `story_scenes`/`story_segments`
   - Add optional pipeline for generated: persist script, run scene extraction + emotion tagging, save segments (without multi-lang)
   - Or: reuse `saveSceneWithSegments` for a synthetic “translation” row for generated

2. **Per-segment TTS**
   - Config exists (`per-segment-tts`); pipeline path not implemented
   - Would enable per-segment `audioUrl` and accurate `durationMs`

3. **Translation-only edit**
   - `updateTranslationContent` without republish leaves stale scenes/audio
   - Options: (a) invalidate scenes for that language, (b) require republish for timeline changes

### Medium Priority

4. **Category field**
   - Add `category` to `library_stories` (distinct from `theme`) if browse-by-category needs it

5. **Subtitle field**
   - Add `subtitle` to segments when display text differs from narration (e.g. abbreviated)

6. **Scene markers from LLM**
   - Extend rewrite prompt to return `{scenes: [{text, backgroundHint}]}` or insert `---` at scene boundaries

7. **Narration script persistence**
   - `StoryProcessingService` does not write to `story_narration_scripts`; `NarrationScriptService` falls back to rewrite
   - Consider persisting script from pipeline to avoid repeated rewrite on manifest fallback

### Low Priority

8. **story_versions for translation edits**
   - StoryVersion only on master `repository.update`; not on `updateTranslationContent`
   - Add `story_translation_versions` if full audit of per-language edits is needed

9. **backgroundHint for generated**
   - Map `story.theme` → `backgroundHint` in runtime manifest when no persisted scenes

---

## 6. Recommended Spring Boot Package Layout

### Current Structure

```
com.tamixa
├── api/                          # REST controllers
│   ├── stream/                   # StreamUrlController, timeline (PlaybackManifest)
│   ├── playback/                 # PlaybackPositionController
│   ├── narration/                # NarrationController
│   ├── library/                  # LibraryStoryController
│   ├── story/                    # StoryController (generated)
│   └── admin/                    # AdminController, stories, etc.
├── application/
│   ├── storyengine/              # Stage 1: StorySceneExtractor
│   ├── narration/                # Stages 2–3: Pipeline, emotion, TTS
│   │   └── impl/                 # TTSServiceImpl, voice strategies
│   ├── playback/                 # Stage 4: PlaybackManifestService
│   ├── storylibrary/             # Library CRUD, invalidation
│   ├── pipeline/                 # StoryProcessingOrchestrator, events
│   ├── story/                    # GeneratedStoryNarrationService
│   ├── translation/              # TranslationClient
│   └── port/                     # RepositoryPort, TtsPort, etc.
├── domain/
│   ├── StoryScene, StorySegment, StoryVersion
│   └── narration/
├── infrastructure/
│   ├── persistence/              # story_scenes, story_segments, JPA adapters
│   ├── narration/                # TTS adapters
│   └── ...
└── common/
```

### Story Engine Stage → Package Mapping

| Stage | Component | Package | Responsibility |
|-------|-----------|---------|----------------|
| **1** | StorySceneExtractor | `application.storyengine` | Normalize: raw → scenes (split by markers), backgroundHint from theme |
| **2** | EmotionTaggingService | `application.narration` | Segment: narration vs dialogue, speaker |
| **3** | StoryProcessingService | `application.narration` | Orchestrate: translate → rewrite → TTS → saveSceneWithSegments |
| **4** | PlaybackManifestService | `application.playback` | Timeline: scenes → manifest JSON |

### Recommended Extensions (Optional Refactor)

| New / Moved | Package | Purpose |
|-------------|---------|---------|
| StoryNormalizationService | `application.storyengine` | Explicit Stage 1 facade (wraps extractor + optional LLM scene detection) |
| StorySegmentationService | `application.storyengine` | Explicit Stage 2 facade (emotion tag → segments) — currently implicit in pipeline |
| — | — | Keep `StoryProcessingService` in `narration` as orchestrator |

**Layering:**
- **api/** — HTTP; thin controllers, delegate to application
- **application/** — Use cases; storyengine (Stage 1–2), narration (Stage 3), playback (Stage 4)
- **domain/** — Entities, value objects; no framework
- **infrastructure/** — JPA, TTS, CDN, external APIs

---

## 7. Summary Checklist

| Item | Status |
|------|--------|
| Library first-time → structured assets | ✅ |
| Library re-publish → replace scenes | ✅ |
| Library content edit → full reset | ✅ |
| StoryVersion on master update | ✅ |
| Scene extraction (markers) | ✅ |
| backgroundHint from theme | ✅ |
| Narration vs dialogue (segment_type) | ✅ |
| Multiple scenes | ✅ |
| storyId as "story_123" | ✅ |
| Generated → structured assets | ✅ |
| Per-segment TTS | ✅ |
| Translation-only edit → invalidate | ✅ |
| Separate category field | ✅ |
| Narration script persistence from pipeline | ✅ |

---

## 8. Recommended Next Steps

**Implemented (2025-03-14):**

1. ✅ **Per-segment TTS** — When `NARRATION_PER_SEGMENT_TTS=true`, pipeline generates seg_0.mp3, seg_1.mp3; manifest uses per-segment audioUrl.
2. ✅ **Translation-only edit** — `updateTranslationContent` now calls `invalidateNarrationAudioForLanguages` when content changes.
3. ✅ **Generated story scenes** — `generated_story_scenes` + `generated_story_segments` tables; GeneratedStoryNarrationService persists; PlaybackManifestService uses when available.
4. ✅ **Persist narration script** — StoryProcessingService saves to `story_narration_scripts` after pipeline; NarrationScriptService returns cached.
5. ✅ **Category field** — Migration V57 adds `library_stories.category`; domain/entity updated.
6. ✅ **backgroundHint for generated** — Runtime manifest maps `story.theme` → backgroundHint when no persisted scenes.
