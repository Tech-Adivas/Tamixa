# Story Flow Verification (Regenerate → Review → Audio)

This document verifies the end-to-end flow from **Regenerate with prompt** through **Submit for review**, **Story for review** (approval), and **Generate audio**, and identifies gaps or improvements.

**Default config:** `AUDIO_AFTER_APPROVAL=true` (Submit does not trigger pipeline; audio only after approval + Generate audio)

---

## Your Steps vs Actual Flow

### 1. Regenerate with prompt → Story converted → Form populated for all languages

| Your expectation | Actual behavior | Status |
|------------------|-----------------|--------|
| Raw story → OpenAI → proper story format | LLM (Tamixa conversion prompt) rewrites source-language story to JSON: `title`, `category`, `theme`, `moral`, `story_text` | ✅ Correct |
| Populated to stories form for all languages | Backend returns `content`, `title`, `moral`, `category`, `theme` + `translations` (en, hi, te, kn, ml). Frontend updates `form` and `translationContentEntries`. | ✅ Correct |
| Nothing saved yet | Regenerate with prompt returns only; no DB write. User must Save or Submit for review. | ✅ Correct |

**Gap:** None.

---

### 2. Submit for review → Story saved in DB

| Your expectation | Actual behavior | Status |
|------------------|-----------------|--------|
| User verifies, then Submit for review | User edits if needed, clicks **Submit for review** | ✅ Correct |
| All story content saved in DB | `updateLibraryStory` saves: `library_stories` (master) + `story_translations` for languages in `translationContentEntries`. When Submit with status=PUBLISHED, `translationContentEntries` from Regenerate are written to `story_translations`. | ✅ Correct |
| Multiple edits saved | **Save (draft)** always persists; no "content unchanged" check for draft. **Submit for review** blocks only when content is unchanged (prevents duplicate submissions). Each edit + Save persists. | ✅ Correct |
| Story goes to Story for review | Status becomes PUBLISHED; `narration_approved_at` is cleared so story reappears in review queue. | ✅ Correct |
| Pipeline runs on Submit? | **No** when `AUDIO_AFTER_APPROVAL=true`. Submit only saves content. | ✅ Per design |

**Gap:** None. Multiple edits can be saved via Save (draft); each Save persists. Ensure `translationContentEntries` is populated (from Regenerate with prompt) before Submit so all languages are saved.

---

### 3. Story for review → Approve for all languages

| Your expectation | Actual behavior | Status |
|------------------|-----------------|--------|
| Converted story visible for review | Story appears in Story for review queue. Content comes from `story_translations` (saved at Submit). | ✅ Correct |
| Approve for all languages | Per-language review: reviewer opens each language (ta, en, hi, te, kn, ml), views content, marks as **reviewed** via `library_story_language_reviews`. **Approve** button enables only when **all** languages are marked reviewed. | ✅ Correct |
| One "Approve for delivery" | Single Approve action sets `narration_approved_at` for the whole story. Does **not** trigger pipeline. | ✅ Correct |

**Gap:** The Approve success toast says *"Pipeline runs (translate + audio)"* — this is **incorrect** when `AUDIO_AFTER_APPROVAL=true`. Approve only sets approval; pipeline runs later in Story to Speech. Consider updating the toast.

---

### 4. Generate audio (Story to Speech)

| Your expectation | Actual behavior | Status |
|------------------|-----------------|--------|
| After approval, story goes to narration | **Narration (Story to Speech)** lists only stories with `narration_approved_at` set. | ✅ Correct |
| Click Generate audio | Triggers `POST /admin/stories/{id}/trigger-pipeline` (or regenerate-narration if audio exists). | ✅ Correct |
| Translate → TTS → audio | Pipeline: for each language: **translate** (if missing) → **rewrite** (LLM conversational script) → **TTS** (audio to S3). | ✅ Correct |

**Gap:** None.

---

### 5. Translated stories populated to Stories form after audio

| Your expectation | Actual behavior | Status |
|------------------|-----------------|--------|
| After audio generated, translations visible in Edit form | Pipeline writes rewritten script to `story_translations.content`. Edit form loads via `getLibraryStory(id, lang)` which returns translation content. | ⚠️ **Requires manual refresh** |
| Auto-populated | No. User must click **"Refresh content"** on the Edit page to reload from server. | ⚠️ Gap |

**Gap:** The translated/rewritten content is **not** auto-refreshed in the Edit form. User must click **"Refresh content"** to see pipeline output in the Other languages tabs.

**Improvement:** Add a hint near "Other languages" like: *"After Generate audio completes, click Refresh content above to see pipeline output."* Or auto-refresh when returning to Edit after pipeline status becomes COMPLETED.

---

### 6. Verify audio for full story

| Your expectation | Actual behavior | Status |
|------------------|-----------------|--------|
| Verify audio covers full story | Story to Speech has Preview (Play) per language. **Automated verification** flags possible truncation. | ✅ Implemented |
| Duration / completeness | When audio duration < 50% of expected (from script word count), `story_narration_audio.truncation_warning` is set. Pipeline status reads from it; Story to Speech UI shows "⚠ Possible truncation: ta,en" in amber. | ✅ Implemented |

**Implemented:** At save time (StoryProcessingService, StoryProcessingOrchestrator), we compare `duration_seconds` with expected from script word count (150 wpm). If audio < 50% of expected, `truncation_warning` is persisted on `story_narration_audio`. Pipeline status aggregates languages where `truncation_warning=true` into `audioCoverageWarnings`; for pre-migration rows (flag=false), a fallback computes from duration vs `readingTimeMinutes`. Story to Speech UI shows "⚠ Possible truncation: Ta, En, Hi" (formatted via LANG_SHORT). When detected, a WARN log is emitted. User can **Regenerate flagged** (only those languages, TTS-only) or **Regenerate all** to retry.

---

### 7. Submit for review while editing

| Your expectation | Actual behavior | Status |
|------------------|-----------------|--------|
| Edit story, then Submit for review | Edit page has **Submit for review** button. Calls `updateLibraryStory` with `status: "PUBLISHED"` and `translationContentEntries` if populated. | ✅ Correct |
| Story reappears in review queue | When status=PUBLISHED, `narration_approved_at` is set to `null`, so story goes back to Story for review. | ✅ Correct |
| Block duplicate Submit | Backend checks: if content unchanged and no translation changes, rejects with "Content unchanged". | ✅ Correct |
| Pipeline running blocks Submit | If pipeline is running for this story, Submit is rejected with "Pipeline is already running". | ✅ Correct |

**Gap:** None. Flow works. One nuance: if user previously **Approved** the story, then edits and **Submits for review** again, `narration_approved_at` is cleared — story goes back to Review queue. Correct.

---

## Summary: Gaps and Improvements

| # | Gap / Improvement | Priority | Action |
|---|--------------------|----------|--------|
| 1 | Approve toast says "Pipeline runs" but with AUDIO_AFTER_APPROVAL it does not | Low | Update toast to: "Story approved for delivery. Go to Narration (Story to Speech) and click Generate audio to create audio." |
| 2 | Edit form does not auto-refresh after pipeline; user must click "Refresh content" | Medium | ✅ Hint added in Other languages section |
| 3 | No automated verification that audio covers full story | Low | ✅ Implemented: audioCoverageWarnings when duration < 50% expected |

---

## Flow Diagram (AUDIO_AFTER_APPROVAL=true)

```
Edit story
    │
    ├─ Regenerate with prompt → LLM + translate → form populated (NOT saved)
    │
    ├─ Save (draft) → DB updated, status stays DRAFT
    │
    └─ Submit for review → DB updated, status=PUBLISHED, narration_approved_at=null
                              translationContentEntries → story_translations
                              └─ NO pipeline
                                    │
                                    ▼
                        Story for review queue
                                    │
                                    ├─ View each language (ta, en, hi, te, kn, ml)
                                    ├─ Mark each as "reviewed" (library_story_language_reviews)
                                    └─ Approve for delivery → narration_approved_at = now
                                          └─ NO pipeline
                                                │
                                                ▼
                        Narration (Story to Speech)
                                    │
                                    └─ Generate audio / Regenerate audio
                                          └─ Pipeline: translate + rewrite + TTS → audio in S3
                                                │
                                                ▼
                        Story READY (audio playable)
                        Edit form: click "Refresh content" to see pipeline output in Other languages
```

---

## Verification Checklist

- [x] Regenerate with prompt populates form for all languages (source + translations)
- [x] Submit for review saves library_story + story_translations for all languages
- [x] Story for review requires per-language "reviewed" before Approve enables
- [x] Approve sets narration_approved_at; does not run pipeline (AUDIO_AFTER_APPROVAL)
- [x] Story to Speech shows only approved stories; Generate audio triggers pipeline
- [x] Pipeline writes to story_translations; Edit form loads via getLibraryStory + "Refresh content"
- [ ] **Verify:** Submit for review works when editing an already-approved story (clears approval, reappears in review)
- [ ] **Verify:** Approve toast message (consider updating for AUDIO_AFTER_APPROVAL mode)
