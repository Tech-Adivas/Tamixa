# Translation & TTS Pipeline

Async pipeline for Tamixa curated story translation and narration (conversational TTS).

## Architecture (New Flow)

```
Admin approves narration (POST /approve-narration)
    → StoryProcessingService.processSync / processLanguagesAsync
    → For each language (ta, en, hi, ...):
         TRANSLATING  → TranslationService (Tamil → target)
         REWRITING    → RewriteService (conversational tone)
         TTS_PROCESSING → TTSService (Google/OpenAI) → S3
         COMPLETED    → story_narration_audio (voice=default, READY)
    → processing_job updated (IN_PROGRESS → COMPLETED | FAILED)
```

## Tables

| Table | Purpose |
|-------|---------|
| `curated_stories` | Tamil source (MASTER) |
| `story_translations` | Translated content + status per (master_story_id, language) |
| `story_narration_audio` | Neural TTS per (translation_id, voice_profile) |
| `processing_job` | Job-level tracking for admin UX (progress, overallStatus) |

Legacy tables `story_audio` and `story_processing_status` have been removed (V47).

## Pipeline Triggers

- **Approve narration**: Admin clicks "Approve narration" → `StoryProcessingService` runs pipeline
- **On-demand cloned voice**: User requests `stream-url?voiceProfile=cloned:{id}` → `StoryProcessingOrchestrator.process`
- **Narration API**: Parent requests narration for a translation → `publishNarrationRequest` → `StoryProcessingOrchestrator`

## Serving

- **Audio**: `story_narration_audio` (canonical); `curated_stories.audio_file_url` is legacy and may be stale
- **GET /v1/stories/curated** → `story_translations` + `story_narration_audio`
- **GET /v1/stories/{id}/stream-url** → signed URL from `story_narration_audio` path

## Pipeline Status (Admin UX)

| Status | Meaning |
|--------|---------|
| PENDING | No translations/audio yet |
| TRANSLATING_LANGUAGES | Translating or rewriting |
| TTS_PROCESSING | Generating TTS |
| READY_FOR_REVIEW | All languages have audio |
| FAILED | Error; show retry |

## Configuration

```yaml
app:
  translation-pipeline:
    source-language: ta
    target-languages: en,hi,te,kn,ml
```
