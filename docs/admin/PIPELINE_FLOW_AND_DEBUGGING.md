# Story Pipeline Flow — Step-by-Step

Use this to trace why a story (e.g. #33) is stuck or taking too long.

---

## 1. How the pipeline is triggered

| Action | What happens |
|--------|--------------|
| **Submit for review** | Admin edits story → clicks "Submit for review" → `StoryLibraryService.update()` runs → `resetPipelineStateForSubmitForReview(id)` deletes all translations + audio → `triggerPipelineExecutor.execute { processSync(id) }` |
| **Run pipeline** (Story library) | Admin clicks ▶ Run pipeline → `POST /stories/{id}/trigger-pipeline` → `triggerPipelineExecutor.execute { processSync(id) }` |
| **Retry** (Pipeline triage) | Admin clicks Retry → `POST /stories/{id}/retry` → `triggerPipelineExecutor.execute { retryFailed(id) }` |

**Important:** "Submit for review" **deletes all translations and audio** first (`resetPipelineStateForSubmitForReview`). The pipeline then starts from scratch for all 6 languages.

---

## 2. Queue and thread pools

```
triggerPipelineExecutor (2–4 threads, queue 16)
    └── processSync(storyId) OR retryFailed(storyId)
            │
            └── processLanguagesInternal(storyId, [ta, en, hi, te, kn, ml])
                    │
                    └── pipelineLanguageExecutor (4 threads) runs 6 tasks in parallel
                            │
                            └── Each task: processLanguage(storyId, lang)
                                    ├── Translation (OpenAI)
                                    ├── Rewrite (pipelineRewriteExecutor, 4 threads) → OpenAI (avoids FJP starvation)
                                    └── TTS (pipelineTtsExecutor, 4 threads) → Google/OpenAI TTS API
```

- **triggerPipelineExecutor**: Up to 4 stories can run their pipeline at once. Others wait in the queue.
- **pipelineLanguageExecutor**: For one story, up to 4 languages run in parallel (6 langs → 4 run, 2 wait for a free thread).
- **pipelineTtsExecutor**: Up to 4 TTS calls at a time (shared across all stories/languages).

---

## 3. Per-language flow (`processLanguage`)

For each language (ta, en, hi, te, kn, ml):

| Step | Status set | What runs | Where it can hang/fail |
|------|------------|-----------|------------------------|
| 1 | `progressTracker.setProcessing(id, lang)` | In-memory "story is on Telugu" | — |
| 2 | `TRANSLATING` | `translationService.translateIfNeeded()` → OpenAI | Rate limit (429), timeout (60s), network |
| 3 | `REWRITING` | `rewriteService.rewrite()` → OpenAI | Rate limit, timeout, rewrite permit (max 5 concurrent) |
| 4 | `TTS_PROCESSING` | `ttsService.synthesize()` → Google/OpenAI TTS | Rate limit, timeout (5 min per lang), TTS permit (max 5) |
| 5 | `COMPLETED` | Save audio, scenes, script to DB | S3/storage errors |

**Timeouts (defaults):**
- Rewrite: 1 min (`rewrite-timeout-minutes`). On timeout → REWRITE_FAILED, retries.
- Per language: 5 min (`language-timeout-minutes`)
- Total for all 6 languages: 60 min (`total-timeout-minutes`)
- TTS inside `processLanguage`: 5 min per call (`future.get(timeoutMinutes)`)

---

## 4. Where story #33 could be stuck

| Symptom | Likely place | How to check |
|---------|--------------|--------------|
| Banner: "Generating Telugu" for 15+ min, no progress | Stuck in step 2, 3, or 4 for Telugu | Backend logs: last `PIPELINE >>>` for story 33 |
| Banner never appears | Queue: story 33 waiting behind others in `triggerPipelineExecutor` | Check how many stories are in PROCESSING |
| Pipeline triage: story 33 not listed | No translation with TRANSLATING/REWRITING/TTS_PROCESSING/TRANSLATION_FAILED/REWRITE_FAILED/TTS_FAILED | Query `story_translations` for `master_story_id=33` |
| Banner shows Telugu but triage doesn't | progress tracker has (33, te) but no DB row yet, or translations all PENDING/COMPLETED | DB: `SELECT * FROM story_translations WHERE master_story_id = 33` |

---

## 5. Key data structures

| Location | Purpose |
|----------|---------|
| **progressTracker** (in-memory) | `storyId → (language, startedAt)`. Banner reads this. Cleared on pipeline finish or after `totalTimeout+15` min (stale). |
| **library_stories.status** | PUBLISHED → PROCESSING (when pipeline starts) → READY (all done) or PUBLISHED (partial/failed) |
| **story_translations** | Per (story, language): status (PENDING, TRANSLATING, …, COMPLETED, TTS_FAILED, …), last_error, retry_count |
| **story_narration_audio** | Per (translation, voice): audio_url, status (READY) |

---

## 6. Debugging story #33

**1. Quick status script**

```bash
./backend/scripts/check-story-status.sh 33
```

Scans logs for story 33, shows per-language DONE/in-progress/FAILED summary, and prints the SQL to run.

**2. Backend logs**

```bash
# In the terminal running bootRun, or:
tail -f backend/logs/tamixa.log | grep -E "masterStoryId=33|storyId=33|story 33"
```

Look for:
- `PIPELINE >>> START masterStoryId=33` — pipeline started
- `processLanguage START masterStoryId=33 lang=te` — started Telugu
- `TTS starting masterStoryId=33 lang=te` — reached TTS step
- `PIPELINE >>> masterStoryId=33 lang=te DONE` — completed
- `PIPELINE >>> masterStoryId=33 lang=te TIMEOUT` or `FAILED` — error

**3. Database**

```sql
-- Story status
SELECT id, title, status FROM library_stories WHERE id = 33;

-- Translation status per language
SELECT id, language, status, last_error, retry_count 
FROM story_translations 
WHERE master_story_id = 33;
```

**4. Banner vs triage**

- **Banner** uses `progressTracker.getActiveStories()` (in-memory).
- **Pipeline triage** uses DB: translations with TRANSLATING, REWRITING, TTS_PROCESSING, or *_FAILED.
- If banner shows "Telugu" but triage is empty: progress tracker has (33, te), but no translation row with those statuses (e.g. crashed before first DB update, or all rows PENDING/COMPLETED).

---

## 7. How to verify the error is from OpenAI (rewrite step)

**1. Call the dev verify endpoint:**

```bash
curl -s "http://localhost:8080/api/v1/dev/verify-connections" | jq '.openaiRewrite'
```

You should see:
- `apiKeySet`: true/false — `true` means `OPENAI_API_KEY` is set
- `readTimeoutMs`: current HTTP read timeout (default 60000)
- `usedBy`: explains that the rewrite step uses OpenAI

**2. Error messages that indicate OpenAI:**
- `"OpenAI API key not configured. Set OPENAI_API_KEY"` — key missing or empty
- `"Rewrite failed: ..."` — from `NarrationLLMException`; the rest is the OpenAI/HTTP error (429, timeout, etc.)
- `"Rewrite timeout after 1min"` — our CompletableFuture timeout; OpenAI may still be hanging

**3. Check your .env:**
- `OPENAI_API_KEY=sk-...` — must be set for real rewrite
- `OPENAI_READ_TIMEOUT_MS=45000` — optional; lower value fails faster (45s)

**4. Backend logs:** Look for `Rewrite failed`, `Narration formatting failed`, or `Upstream rate limit (429)` — all indicate OpenAI path.

---

## 8. Common failure modes

| Cause | Effect |
|-------|--------|
| **OpenAI/Google rate limit (429)** | Translate or Rewrite or TTS fails → retry with backoff (2s, 5s, 10s); after 3 retries → TRANSLATION_FAILED / REWRITE_FAILED / TTS_FAILED |
| **TTS API slow or hanging** | Stuck at `future.get(5, MINUTES)` until timeout → TTS_FAILED |
| **Backend restart mid-pipeline** | progressTracker cleared; DB may have PROCESSING + translation in TRANSLATING/REWRITING/TTS_PROCESSING. Banner goes away; story stays "stuck" in DB until retry |
| **Queue backup** | Many stories submitted → triggerPipelineExecutor queue full → story 33 waits; pipelineLanguageExecutor and pipelineTtsExecutor also shared |
| **ForkJoinPool starvation** (fixed) | Rewrite used FJP.commonPool(); in containers (low CPU) or when other async work saturates it, rewrite tasks could wait indefinitely. Now uses dedicated `pipelineRewriteExecutor`. |

---

## 8. Recovery actions

| Action | When to use |
|--------|-------------|
| **Clear stuck #33** (banner) | Banner shows Telugu 15+ min with no progress. Clears progress tracker so you can run pipeline again. |
| **Run pipeline** (Story library) | After Clear stuck, or when story is PUBLISHED/PROCESSING but pipeline never finished. |
| **Retry** (Pipeline triage) | Story has TRANSLATION_FAILED/REWRITE_FAILED/TTS_FAILED; retries only failed/missing languages. |
