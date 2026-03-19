# Story → Audio → Avatar: Enterprise Flow Reference

This document describes the end-to-end flow from story creation through audio generation to avatar video, with config options and enterprise-grade practices.

---

## Flow Overview

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│ Create/Edit  │────▶│ Submit for   │────▶│ Story for    │────▶│ Story to     │
│ Story        │     │ review       │     │ review       │     │ Speech       │
└──────────────┘     └──────────────┘     └──────────────┘     └──────────────┘
       │                     │                     │                     │
       │                     │                     │                     │
       ▼                     ▼                     ▼                     ▼
  Save draft or        Saves to DB;         Approve sets          Generate audio
  Submit (publish)     clears approval       narration_            (pipeline:
                                       approved_at               translate→
                                                                rewrite→TTS)
                                                                     │
                                                                     ▼
                                                            ┌──────────────┐
                                                            │ Avatar video │
                                                            │ (HeyGen/D-ID)│
                                                            └──────────────┘
                                                                     │
                                                                     ▼
                                                            Lazy: first stream
                                                            request triggers
                                                            avatar generation
```

---

## Config Matrix

| Config | Default | Submit for review | Approve | Generate audio (Story to Speech) |
|--------|---------|-------------------|---------|----------------------------------|
| **AUDIO_AFTER_APPROVAL=true** | ✅ | Saves content only; no pipeline | Sets approval only; no pipeline | **Triggers pipeline** (translate → rewrite → TTS) |
| **AUDIO_AFTER_APPROVAL=false** | | Resets audio, runs full pipeline | Sets approval only; content already exists | Manual trigger if needed |
| **PIPELINE_ON_SUBMIT_ONLY=true** | ✅ | Pipeline runs only when Submit used (if AUDIO_AFTER_APPROVAL=false) | — | — |
| **PIPELINE_ON_SUBMIT_ONLY=false** | | Publish/update also triggers pipeline | — | — |

**Recommended (default):** `AUDIO_AFTER_APPROVAL=true` — content approval before audio; explicit trigger in Story to Speech for cost control and quality gating.

---

## Story Statuses

| Status | Meaning |
|--------|---------|
| DRAFT | Saved; not submitted |
| PUBLISHED | In review queue; awaiting approval |
| PROCESSING | Pipeline running (translate/rewrite/TTS) |
| READY | All languages have audio; ready for delivery |
| CHANGES_REQUESTED | Sent back to author |
| REJECTED | Final rejection |

---

## Audio Pipeline

### Steps (per language)

1. **Translate** — Source content → target language (if missing)
2. **Rewrite** — Conversational script for TTS (LLM)
3. **TTS** — Neural TTS → S3 (`story_narration_audio`)

### Translation pipeline statuses

| Status | Meaning |
|--------|---------|
| PENDING | Not started |
| TRANSLATING / REWRITING / TTS_PROCESSING | In progress |
| TRANSLATION_FAILED / REWRITE_FAILED / TTS_FAILED | Failed; retry available |
| COMPLETED | Audio ready |

### API endpoints

| Endpoint | Purpose |
|----------|---------|
| `POST /admin/stories/{id}/trigger-pipeline` | Start full pipeline (all languages) |
| `POST /admin/stories/{id}/regenerate-narration` | TTS-only; body `{ "languages": ["en","hi"] }` for selective |
| `POST /admin/stories/{id}/republish` | Full pipeline for selected languages |
| `POST /admin/stories/{id}/retry` | Retry failed/missing languages |
| `GET /admin/pipeline/status-batch?ids=1,2,3` | Batch pipeline status |

### Regenerate vs Republish

| Operation | Invalidate | Pipeline |
|-----------|------------|----------|
| **Regenerate** | Selected or all languages | TTS-only (reuse script/content) |
| **Republish** | Selected or all languages | Full (translate → rewrite → TTS) |
| **Retry** | None | Failed/stuck languages only |

---

## Avatar Video

### Trigger

Avatar video is **lazy**: generated on first stream request when:
- Parent has avatar image
- Audio exists for the story+language+voice
- No READY avatar video yet

### Statuses

| Status | Meaning |
|--------|---------|
| NONE | Not entitled or no record |
| PENDING / PROCESSING | Generation in progress |
| READY | Video available |
| FAILED | Generation failed; next request deletes FAILED and retriggers |

### Providers

HeyGen (primary) → Replicate (SadTalker) → D-ID → Gooey

### Admin

- `DELETE /admin/stories/{id}/avatar-video?parentId=&language=&voiceProfile=` — delete and allow regeneration
- Voice & Avatar Studio: test avatar with selected parent/language/voice

---

## Enterprise Practices

### Consistency

1. **Status visibility** — Pipeline status polled every 3s; merge logic accepts all backend updates (including COMPLETED → in-progress after regenerate).
2. **Progress tracker** — `setProcessing("starting")` before pipeline so banner shows immediately.
3. **Selective regenerate** — When truncation warning shows, "Regenerate flagged" only reprocesses those languages.

### Partial failures

- Per-language isolation: one language failing does not block others.
- Pipeline status includes per-language errors; use **Retry** for failed languages.
- Avatar: per (story, parent, language, voice); one failure does not affect others.

### Retries

- **Audio**: Per-language retry limit (default 3); manual Retry resets count for languages with no audio.
- **Avatar**: FAILED records auto-retried on next stream request (delete + re-trigger).
- **Stuck pipeline**: Clear stuck after 5+ min via banner or `POST /admin/pipeline/clear-stuck/{storyId}`.

### Observability

- Pipeline status: `overallStatus`, `progress`, `processing`, `audioCoverageWarnings`, `generatedAtIst`
- Banner: "Background pipeline running — Generating X for story #Y"
- Logs: `PIPELINE >>>` prefixed; truncation warnings at WARN
- `processing_job` table (when enabled): job history per story

### Idempotency

- **Pipeline**: Skips language when READY audio exists and content unchanged.
- **Regenerate**: Invalidates first, then TTS-only; safe under concurrent triggers (last wins).
- **Avatar**: `DataIntegrityViolationException` on duplicate PENDING; safe to retry.

---

## Related docs

- [STORY_FLOW_VERIFICATION.md](STORY_FLOW_VERIFICATION.md) — Regenerate → Submit → Approve → Audio verification
- [STORY_PIPELINE_FLOW.md](STORY_PIPELINE_FLOW.md) — When pipeline runs (config-dependent)
- [PIPELINE_FLOW_AND_DEBUGGING.md](PIPELINE_FLOW_AND_DEBUGGING.md) — Triggers, queues, debugging
- [../runbooks/AVATAR_VIDEO_TROUBLESHOOTING.md](../runbooks/AVATAR_VIDEO_TROUBLESHOOTING.md) — Avatar failures and recovery
