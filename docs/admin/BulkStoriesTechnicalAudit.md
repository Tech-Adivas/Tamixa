# Bulk Stories – Technical Audit (Expected Flow vs Implementation)

Senior technical verification that the bulk stories flow is implemented as per the expected design.

---

## Expected Flow (Contract)

1. **Admin** opens Bulk story generator, selects languages (default Tamil), categories (default Friendship), count 1–25 (default 25), optional "Submit for review after generation". Clicks Generate.
2. **Frontend** sends `POST /api/v1/admin/stories/bulk-generate` with body `{ languages: string[], categories: string[], totalStories: number, publish?: boolean }`.
3. **Backend** validates/coerces input (languages default `["ta"]`, totalStories 1–25, publish boolean), then for each of N stories: build prompt → call OpenAI (max 2048 tokens) → parse JSON (title, theme, category, story_text, moral) → validate non-empty story_text → **run story through content moderation** (StoryModerationService); if rejected, add to failed and skip save → otherwise create library story + initial story_translation row (duplicate title retry with " (idx)") → on publish and pipelineOnSubmitOnly, enqueue processSync(id) per created id. Returns `{ requested, createdCount, failedCount, publish, created[], failed[] }`.
4. **Frontend** shows result; if any created, shows "View in Stories" and (if publish) "Go to Story for review". If 0 created and some failed, shows error toast and empty-state message.
5. **Stories** appear in Stories list; if publish, also in Story for review (newest first). Content for the story’s language is visible from creation (initial translation row). Other languages after pipeline.

---

## Verification Checklist

| # | Item | Status | Notes |
|---|------|--------|-------|
| 1 | POST body: languages, categories, totalStories, publish | ✅ | AdminController parses; defaults languages to ["ta"], totalStories to 25, publish to false. |
| 2 | totalStories coerced to 1–25 | ✅ | Backend: (totalStoriesRaw?.coerceIn(1, 25)) ?: 25. Frontend: totalRequested = Math.min(25, Math.max(1, Number(totalStories) \|\| 25)). |
| 3 | Empty languages → ["ta"] | ✅ | Controller: if (rawLangs.isEmpty()) listOf("ta"). |
| 4 | Empty categories → default in service | ✅ | Service: safeCategories.ifEmpty { listOf("Friendship") }. Controller passes categories as-is; service applies default. |
| 5 | Prompt: output language + no Hindi unless Hindi | ✅ | bulkPromptLanguageInstruction() and Output Format line. |
| 6 | OpenAI call with 2048 max_tokens for bulk | ✅ | openAI.generateStory(prompt, 2048). |
| 7 | JSON parse: title, theme, category, story_text, moral | ✅ | extractStoryTextFromBulkJson for story_text; path("title") etc. for others. |
| 8 | story_text non-empty or throw | ✅ | if (storyText.isBlank()) throw IllegalArgumentException(...). |
| 8b | Story review (moderation) before save | ✅ | storyModeration?.moderateBeforeSave(storyText, ModerationContext("bulk-{idx}", language, 7)); on rejection add to failed with content_moderation. |
| 8c | estimated_duration_seconds (or legacy estimated_duration) → readingTimeMinutes | ✅ | parseEstimatedMinutesFromBulkJson(); create(..., estimatedReadingMinutes = estimatedMinutes). Optional param on create(); fallback word-count-derived. |
| 8d | Bulk progress log per story | ✅ | log.info("Bulk story generation {idx+1}/{total} lang= category="). |
| 9 | create() = library story + initial story_translation | ✅ | After repository.save(story), storyTranslationRepository.save(initialTranslation) for saved.language. |
| 10 | Duplicate title: retry with " (idx+1)" | ✅ | try/catch IllegalArgumentException "already exists", retry create with uniqueTitle. |
| 11 | Pipeline trigger only when publish && pipelineOnSubmitOnly | ✅ | if (publish && appProperties.translationPipeline.pipelineOnSubmitOnly) { createdIds.forEach { triggerPipelineExecutor.execute { processSync(id) } } }. |
| 12 | Response: requested, createdCount, failedCount, publish, created, failed | ✅ | mapOf("requested" to totalRequested, "createdCount" to created.size, ...). |
| 13 | Frontend: validation (at least one lang/cat, total ≤ 25) | ✅ | handleGenerate checks selectedLanguages.length, selectedCategories.length, totalRequested ≤ 25. |
| 14 | Frontend: success toast only when createdCount > 0 | ✅ | if (res.createdCount > 0) showSuccess; else if (res.failedCount > 0) showError; else showSuccess (no stories requested). |
| 15 | Frontend: result card with requested/created/failed, empty state, links | ✅ | Result card; "All generations failed" when createdCount===0 && failedCount>0; "View in Stories" and "Go to Story for review" when publish. |
| 16 | Long-run hint when totalRequested > 5 | ✅ | "Bulk generation may take 2–5 minutes. Do not close or refresh this page." |
| 17 | Pending-review order: newest first | ✅ | findByStatusPublishedAndNarrationApprovedAtNull ORDER BY c.id DESC. |
| 18 | findByIdAndLanguage: content from translation or master | ✅ | Tamil: taTranslation?.content ?: master.content; title same. Other langs: translation.title/content or empty. |
| 19 | Audit log for bulk run | ✅ | recordAdminAuditAction(adminEmail, "bulk_generate_stories", "story", null, "requested=... created=... failed=... publish=..."). |
| 20 | @PreAuthorize on bulk endpoint | ✅ | hasAnyRole('ADMIN','SUPER_ADMIN','CONTENT_MANAGER','REVENUE_ANALYST','SUPPORT'). |
| 21 | Error response 400 on IllegalArgumentException | ✅ | catch (e: IllegalArgumentException) ResponseEntity.badRequest().body(mapOf("message" to ...)). |
| 22 | API client: parse error body and throw with message | ✅ | r.ok check; err?.message ?? ...; throw new Error(msg). |

---

## Edge Cases Verified

- **Combinations empty**: safeLanguages and safeCategories have defaults, so combinations is never empty.
- **All 25 fail**: Backend returns 200 with createdCount=0, failedCount=25. Frontend shows error toast and empty-state message.
- **Partial success**: Backend returns created and failed arrays; frontend shows both; success toast reflects createdCount.
- **Duplicate title**: First story saved; second with same title retries with " (2)" and saves.
- **Tamil script validation**: create() runs validateTamilScript only when language == "ta"; bulk can generate en/hi/etc. without Tamil check.
- **Moderation rejection**: If StoryModerationService rejects story_text, that story is not saved; it is added to failed with error "content_moderation: ...". Other stories in the batch are unaffected.

---

## Files Touched (Implementation)

| Area | File |
|------|------|
| Admin UI | `admin/src/app/(dashboard)/dashboard/stories/bulk-generate/page.tsx` (async + poll, progress "Story 5/25", 409 handling) |
| Admin API | `admin/src/lib/api.ts`, `admin/src/types/api.ts` (bulkGenerateLibraryStoriesAsync, getBulkGenerateJobStatus, job types) |
| Backend API | `backend/.../AdminController.kt` (bulk-generate with guard, bulk-generate/async, bulk-generate/jobs/{jobId}) |
| Backend job store | `backend/.../BulkJobState.kt`, `backend/.../BulkJobStore.kt` (in-memory job state + one-at-a-time guard) |
| Backend service | `backend/.../StoryLibraryService.kt` (create + initial translation, generateBulk with progressCallback, prompt, duplicate title, extractStoryTextFromBulkJson) |
| Backend OpenAI | `backend/.../OpenAIPort.kt`, `OpenAIClient.kt` (generateStory(prompt, maxTokens)) |
| Review order | `backend/.../LibraryStoryJpaRepository.kt` (findByStatusPublishedAndNarrationApprovedAtNull ORDER BY id DESC) |

---

## Optional / Future (implemented)

- **Async bulk (job id + poll)**: POST `/stories/bulk-generate/async` returns 202 with `{ jobId }`; GET `/stories/bulk-generate/jobs/{jobId}` returns status, `progress: { current, total }`, and `result` when COMPLETED/FAILED. Admin UI uses async by default and polls every 2s, showing "Story 5/25" on the button.
- **Progress**: Progress is included in the job status response (`currentIndex`, `requestedTotal`); UI shows "Story current/total" while polling.
- **One bulk at a time**: Backend guard via `BulkJobStore.tryAcquire` / `release`. Sync and async both acquire; if another run is in progress, API returns 409 with message. UI shows error on 409 and note "Only one bulk run at a time."

## Improvements applied (non-breaking)

- **Progress logging**: Each bulk story logs `Bulk story generation {idx+1}/{total} lang= category=` at info level for observability.
- **estimated_duration_seconds from JSON**: Bulk JSON may include `estimated_duration_seconds` (and legacy `estimated_duration`); when present, it is parsed and passed to `create(estimatedReadingMinutes = ...)` so `readingTimeMinutes` reflects the model’s estimate (clamped 0.5–30 min).
- **beforeunload on submit**: Admin bulk-generate page registers a `beforeunload` handler while the request is in progress to reduce accidental tab close/refresh during long runs.
