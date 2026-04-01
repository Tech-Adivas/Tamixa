# Bulk Stories Implementation – Flow Review

Senior review (Mobile dev, AI/ML, UI/UX) of the bulk story generation flow: gaps, risks, and improvements.

---

## 1. Flow Summary

| Stage | Component | What happens |
|-------|-----------|--------------|
| **Trigger** | Admin → Bulk story generator | User selects languages, categories, count (1–25), optional "Submit for review after generation". Clicks Generate. |
| **Request** | Admin API client | `POST /api/v1/admin/stories/bulk-generate` with `{ languages, categories, totalStories, publish }`. |
| **Backend** | AdminController | Parses body, defaults languages to `["ta"]`, totalStories 1–25. Calls `StoryLibraryService.generateBulkStoriesWithTemplatePrompt()`. |
| **Generation** | StoryLibraryService | For each of N stories: build prompt (language, category, combined situation), call `openAI.generateStory(prompt)` (single user message, no system message), parse JSON (`title`, `theme`, `category`, `story_text`, `moral`), validate non-empty `story_text`, then `create()` library story + initial `story_translation` row. |
| **Pipeline** | If publish=true | For each created id, `triggerPipelineExecutor.execute { processSync(id) }` (translate → rewrite → TTS per language). Runs in background (2–4 threads, queue 16). |
| **Response** | AdminController | Returns `{ requested, createdCount, failedCount, publish, created[], failed[] }`. |
| **Post-flow** | Admin | User sees result; can go to Stories list or Story for review. Newest stories first in pending-review (id DESC). |
| **Mobile** | App | Library stories come from `/stories/library`; only **approved** stories are returned. Bulk-created stories appear after approval in Story for review. No change needed on mobile for bulk. |

---

## 2. Gaps & Issues

### 2.1 UI/UX

| Issue | Severity | Description |
|-------|----------|-------------|
| **No progress during generation** | Medium | User clicks Generate and waits. For 25 stories the backend can take **2–5+ minutes** (25 × OpenAI call). No progress bar or “N of 25” updates. |
| **Client timeout risk** | High | Browser/fetch often time out (e.g. 60–120s). Backend runs 25 sequential OpenAI calls in one request; request can exceed client timeout and user sees generic failure. |
| **No “what’s next” after success** | Low | Result card shows created/failed counts and list of created stories but no clear CTA: “View in Stories” / “Go to Story for review”. |
| **Empty state when all fail** | Low | If `createdCount === 0` and `failedCount > 0`, message could be more helpful (“All generations failed. Check OPENAI_API_KEY and try fewer stories.”). |
| **Accessibility** | Low | Number input and checkboxes are usable; ensure “All” and group labels are correctly associated (e.g. `aria-label` / `id`/`htmlFor`). |
| **Link from result to story** | Low | Created items show id, language, category, title but are not clickable to open the story in Stories or Review. |

### 2.2 Backend / AI

| Issue | Severity | Description |
|-------|----------|-------------|
| **Max tokens too low for long stories** | High | `generateStory()` uses `app.openai.max-tokens` (default **1024**). Prompt asks for 1500–1750 words. Output can be truncated and invalid JSON or cut-off story_text. |
| **Duplicate title fails entire story** | Medium | `create()` checks `existsByTitle(title)`. If the model returns the same title twice (e.g. “Friendship Story 1”) the second save throws and that story is counted as failed. |
| **No idempotency / rate limit for bulk** | Low | Two admins (or double-click) can run bulk at once; OpenAI rate limits may hit. No per-admin or global “one bulk run at a time” guard. |
| **estimated_duration_seconds parsed** | Low | Prompt asks for `estimated_duration_seconds` in JSON (legacy `estimated_duration` still supported); backend parses it into `readingTimeMinutes`. Optional improvement: display it in UI. |
| **Single prompt = no system message** | Low | Bulk uses one user message. Tamixa system persona is only in single-story flow. Quality is acceptable but could be aligned with `StoryPromptBuilder` for consistency. |
| **Error message to frontend** | Low | On failure (e.g. IllegalArgumentException), backend returns 400 with `message`. Some errors (e.g. “A story with this title already exists”) are user-actionable; others (e.g. “Model returned empty story_text”) are operational. |

### 2.3 Pipeline & Content

| Issue | Severity | Description |
|-------|----------|-------------|
| **Pipeline queue cap** | Medium | `triggerPipelineExecutor` has queue capacity 16. Bulk with publish=true can enqueue 25 `processSync` tasks; excess run in caller thread (log: “Trigger pipeline queue full”). Could delay or block. |
| **Content visibility** | Done | Initial `story_translation` row for story language is created on `create()`, so content shows for that language even before pipeline. Other languages still need pipeline. |
| **Review table order** | Done | Pending-review ordered by `id DESC` so newest (e.g. bulk-created) appear first. |

### 2.4 Mobile

| Issue | Severity | Description |
|-------|----------|-------------|
| **None** | — | Library API unchanged. Only approved stories are shown. Bulk only affects admin-created library stories; after approval they appear like any other. |

---

## 3. Recommendations

### 3.1 High priority

1. **Avoid client timeout**
   - **Option A:** Show a clear message: “Bulk generation can take 2–5 minutes. Do not close this page.”
   - **Option B:** Make bulk async: POST returns a job id, frontend polls for status and result (recommended for 25 stories).
2. **Increase max_tokens for bulk**  
   Use a higher limit for bulk generation (e.g. **2048**) so 1500–1750 word stories are not truncated. Either:
   - add `openai.bulk-max-tokens` and use it in a bulk-specific path, or
   - raise default `OPENAI_MAX_TOKENS` and document that bulk needs it.
3. **Duplicate title handling**  
   In the bulk loop, if `create()` throws due to duplicate title, retry once with a disambiguated title (e.g. `"$title (${idx + 1})"`) so one duplicate does not fail the whole story.

### 3.2 Medium priority

4. **Progress indicator**  
   If keeping synchronous bulk: consider Server-Sent Events (SSE) or WebSocket so the backend can push “Story 5/25 created” and the UI can show progress. Otherwise, at least show “Generating… This may take several minutes.”
5. **Post-success navigation**  
   After success, show primary actions: “View in Stories” and “Go to Story for review” (when publish=true).
6. **Pipeline queue**  
   Consider increasing trigger executor queue (e.g. to 32) or making “publish” path enqueue a single “process all these ids” job instead of one per id, so queue does not overflow on large bulk runs.

### 3.3 Low priority

7. **Optional UI**: display the model’s estimated duration/reading time (backend already parses `estimated_duration_seconds`).
8. **Empty/failure state copy** when `createdCount === 0`: suggest checking API key and reducing count.
9. **Accessibility**: ensure labels and live region for result summary (e.g. “Created 25 stories, 0 failed”).
10. **Clickable created rows**: link id/title to `/dashboard/stories?id=...` or open view modal.

---

## 4. Files Touched by Bulk Flow

| Area | File(s) |
|------|--------|
| Admin UI | `admin/src/app/(dashboard)/dashboard/stories/bulk-generate/page.tsx` |
| Admin API | `admin/src/lib/api.ts`, `admin/src/types/api.ts` |
| Backend API | `backend/.../AdminController.kt` (bulk-generate, pending-review order) |
| Backend service | `backend/.../StoryLibraryService.kt` (create, generateBulkStoriesWithTemplatePrompt, prompt, extractStoryTextFromBulkJson, initial translation) |
| Backend OpenAI | `backend/.../OpenAIClient.kt` (generateStory), `application.yml` (openai.max-tokens, read-timeout-ms) |
| Pipeline | `backend/.../StoryProcessingService.kt`, `NarrationAsyncConfig.kt` (triggerPipelineExecutor) |
| Review table | `backend/.../LibraryStoryJpaRepository.kt` (findByStatusPublishedAndNarrationApprovedAtNull order) |
| Mobile | No code changes; library API unchanged. |

---

## 5. Checklist for Next Iteration

- [ ] Timeout / long-run UX: message or async job + polling
- [ ] Bulk max_tokens ≥ 2048 (config or code)
- [ ] Duplicate title retry or unique title in bulk
- [ ] “View in Stories” / “Story for review” after success
- [ ] Optional: progress (SSE/WS) or “may take several minutes”
- [ ] Optional: trigger executor queue size or batch job
- [ ] Optional: better empty/failure copy, a11y, clickable rows
