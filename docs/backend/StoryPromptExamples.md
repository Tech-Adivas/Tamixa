# Story generation prompt (Tamixa)

The story generation flow uses the **Tamixa** storyteller persona: lead story designer for a premium children's audio product. Prompts are expert-grade: clear role, quality bar, security & compliance, tone calibration, and strict output contract.

## System message (static, cacheable)

See `StoryPromptBuilder.buildSystemMessage()` for the canonical implementation. Structure:

- **Role & mandate**: Tamixa = lead story designer; output used for TTS, voice cloning, family listening; quality and safety non-negotiable; publication-ready (no filler, every sentence earns its place).
- **Task**: Generate exactly one original story in the requested language; output only valid JSON; no commentary, no markdown.
- **Quality bar**: Publication-ready prose; intellectually rich; mild conflict resolved peacefully; positive ending; category/theme/tone coherent and age-appropriate.
- **Language & clarity**: English as structural reference; one idea per sentence; clear subject and verb; short to medium length; natural connectors; easy to read aloud. Vocabulary: varied, precise, age-appropriate; native terms; no jargon.
- **Tone & voice calibration (Tamixa voice)**: Magical, comforting, joyful. Gentle authority—like a trusted parent, teacher, or grandparent. Warm, friendly, emotionally gentle. No sarcasm, cynicism, fear, or harshness. Safe to hear: reassuring, uplifting, memorable.
- **Narration structure**: Short blocks (1–3 sentences); natural transitions; Storytelling Script (narrator + dialogue) with tone markers ([Pause 500ms], [Happy tone], [Soft voice], etc.); SSML/OpenAI TTS ready.
- **Cultural context (Indian)**: Indian folklore and mythology (e.g. Tenali Rama, Panchatantra, regional tales); festival themes (Diwali, Pongal, Onam, Ugadi) in the story's native language with appropriate terms; village/town/nature settings; culturally familiar names and values; family-friendly and inclusive.
- **Educational perspective (Tamixa differentiator)**: Every story is designed to support learning as well as enjoyment. Age-banded learning (cause-and-effect for 1–4; empathy, sequencing, wonder words for 5–7; inference and perspective-taking for 8–12). SEL: characters name feelings; kind/brave choices; conflict resolved through understanding. Wonder words: 2–4 age-appropriate rich words in context. Inference-friendly: moments that reward paying attention. One clear problem, one earned resolution. NEP/life-skills aligned (critical thinking, collaboration, values) implicitly in the plot. Optional **learningFocus** in the user request can emphasize: empathy, problem_solving, vocabulary, curiosity, perseverance, sharing, honesty, courage, kindness, friendship, responsibility.
- **Security & compliance (mandatory — zero tolerance)**: Child safety (suitable for under 12; no violence, gore, horror, abuse, fear, dangerous imitation, self-harm, substances). Indian compliance (no disparagement of religion/community/region/language; no political/hateful/discriminatory content). Positive values only; conflicts resolved peacefully. Prohibited: violence, weapons, death, war, politics, religious conflict, drugs, alcohol, self-harm, advertising, real celebrities/places, adult themes.
- **Length & multilingual naturalness**: ~1500–1750 words; language-specific grammar and vocabulary (Tamil, Hindi, Telugu, Kannada, Malayalam, English).
- **Output contract**: Single valid JSON only; keys exactly title, category, theme, moral, story_text, estimated_duration_seconds; no markdown/code fence; before responding confirm no prohibited content, language/grammar, valid JSON.

## User prompt (conversational)

The user prompt injects **language**, **child name**, **age**, **theme**, and optional extras (interests, favorite color/animal, custom request, **learningFocus**) from the request. Do not hardcode age or child name. **learningFocus** (optional): one of empathy, problem_solving, vocabulary, curiosity, perseverance, sharing, honesty, courage, kindness, friendship, responsibility; when present and allowed, adds a one-line hint so the story emphasizes that dimension.

Example shape (Tamil, theme "friendship"):

```
Generate a story in ta based on this request: for my child {childName} (age {age}), about friendship. Warm, positive, and age-appropriate. Use short paragraphs (1–3 sentences), warm and narration-friendly.

Use clear, everyday words. Short to medium sentences.
Set the story in a Tamil-friendly context: family, village or town, respect for elders, friendship, nature...

Aim for about 1500–1750 words; keep it within the configured maximum word cap (e.g. $maxWords). Return only valid JSON: title, category, theme, moral, story_text, estimated_duration_seconds (number, in seconds). No markdown, no code block.
```

## Fallback user prompt (on parse failure)

Same structure as the conversational user prompt. Used when the primary response fails validation or parse.

## Expected JSON output

Backend requires at least: `title`, `moral`, `story_text`, `estimated_duration_seconds`. Optional: `category`, `theme`.

**story_text** should be in Storytelling Script format (narrator + dialogue) with optional tone/pause markers for narration:
- Narrator lines plus dialogue (e.g. `"Character: dialogue text"`).
- Inline markers: `[Pause 500ms]`, `[Pause 1s]`, `[Happy tone]`, `[Soft voice]`, `[Warm tone]`, `[Calm]`, `[Whisper]`, `[Excited]`.
- Overall feel: magical, comforting, joyful. Output suitable for SSML and OpenAI TTS.

```json
{
  "title": "நண்பர்கள்",
  "category": "friendship",
  "theme": "kindness",
  "moral": "Sharing makes everyone happy.",
  "story_text": "[Story content in requested language]",
  "estimated_duration_seconds": 600
}
```

## Educational perspective (unique differentiator)

Tamixa stories are designed to be both entertaining and developmentally meaningful—a differentiator from entertainment-only competitors.

- **Built into every story**: Age-banded learning design, SEL (characters naming feelings, kind/brave choices), 2–4 "wonder words" in context, inference-friendly moments, one clear problem and earned resolution, NEP/life-skills alignment.
- **Optional learning focus**: The API accepts `learningFocus` in the story generation request. Allowed values (case-insensitive, spaces normalized to underscore): `empathy`, `problem_solving`, `vocabulary`, `curiosity`, `perseverance`, `sharing`, `honesty`, `courage`, `kindness`, `friendship`, `responsibility`. When provided, the user prompt adds a one-line hint so the model emphasizes that dimension. Invalid values are ignored.

## Age-based vocabulary hints

| Age band | Hint |
|----------|------|
| 1–4      | Use very simple words and short sentences. No complex ideas. |
| 5–7      | Use clear, everyday words. Short to medium sentences. |
| 8–12     | You may use a richer vocabulary; still suitable for children. |

## English as primary; sentence clarity in all languages

Stories are structured for **clarity using English as the reference**: short, clear sentences; one idea per sentence; clear subject and action. When the output language is not English, the model applies those same clarity standards and then writes the story in the target language (Tamil, Hindi, etc.) so that every sentence remains clear and easy to narrate. This keeps sentence clarity consistent across all languages.

**Sentence clarity rules (all languages):** One idea per sentence; clear subject and verb; prefer short to medium sentences; no run-ons; every sentence easy to read aloud and understand in one pass.

**Vocabulary improvement (all languages):** Use varied vocabulary and avoid repeating the same word in close succession; use natural synonyms where they fit. Choose precise, concrete, age-appropriate words; avoid vague or weak wording. Prefer the language's everyday and native terms so the story sounds natural when spoken. Keep vocabulary consistent with a warm, storytelling tone; no jargon or overly formal terms.

## Language / culture, grammar, and vocabulary

Each language has its own grammar, vocabulary, and style. Prompts instruct the model to use that language’s rules, natural vocabulary, and style:

- **ta / Tamil**: Tamil script; correct verb suffixes (past/present/future), SOV order; everyday Tamil words, native terms, prefer Tamil over loanwords where natural; proper pronouns and number. Tamil-friendly context: family, village, respect for elders.
- **en / English**: Standard English grammar (SVO), correct tense and agreement; clear, everyday words; age-appropriate; conversational narration. Culturally neutral and family-friendly.
- **hi / Hindi**: Devanagari script; correct verb conjugation and postpositions; common Hindi words and native expressions; honorifics where appropriate. Conversational audiobook style.
- **te / Telugu**: Telugu script; correct verb forms and case suffixes; natural SOV order; everyday Telugu words and native expressions; age-appropriate.
- **kn / Kannada**: Kannada script; correct verb conjugation and case markers; common Kannada words and native expressions; natural storytelling rhythm.
- **ml / Malayalam**: Malayalam script; correct verb forms and agglutination; everyday Malayalam words and native vocabulary; age-appropriate; avoid literal translation.

## Regenerate with prompt (all languages)

Admin "Regenerate with prompt" (edit story) transforms the current story content with the default Tamixa conversion prompt and returns the rewritten content, title, moral, and **category/theme**. The prompt instructs the LLM to choose an appropriate **category** from the app’s canonical list (Animals, Friendship, Adventure, Village Life, Moral Stories, Funny Stories, Family Stories, Fantasy, Nature, Bravery) based on the story; the backend normalizes it and returns it so the edit form can update the story’s category. By default it also **generates story content for all other languages** (en, hi, te, kn, ml): the backend calls `TranslationService.translateIfNeeded` for each target language and returns a `translations` map in the response. If a language had no story content before, the translated content is created and shown in the edit form; the admin can then Save or Submit for review so the pipeline can run rewrite + TTS for each language. Request body can include `"generateForAllLanguages": false` to skip generating other languages (legacy behaviour: only source-language transform).

The conversion prompt also aims for the full-length target: roughly **1500–1750 words** (about **10–12 minutes** of TTS at natural pacing), while staying within any configured maximum word cap enforced by validation in the pipeline.

## Translate + rewrite process (in the pipeline)

The narration pipeline runs **per language** (source + target languages). For each language it does:

1. **Translate** (`runTranslationStep` → `TranslationService.translateIfNeeded`): Takes the source-language story (title, content, moral) and produces a **translation** in the target language. Used for non–source languages (e.g. Tamil → English, Hindi, etc.). If a translation row already exists with valid content, that content is reused and the step only updates status to REWRITING. Result is stored in **`story_translations`** (content, title, moral, word_count, reading_time_minutes) per language. The admin frontend can fetch and show this via **`GET /api/v1/admin/stories/:id?language=...`** (View story → language tabs).
2. **Rewrite** (`runRewriteStep` → `RewriteService.rewrite`): Takes the (translated or source) story text and produces a **conversational narration script** via the LLM (Tamixa style, tone, SSML-friendly). This is the text that will be spoken; it may add pauses, tone hints, and natural phrasing. Result is the script used for TTS.
3. **TTS**: SSML is built from the script, sent to the TTS service, and the audio is uploaded to S3 and linked in `story_narration_audio`. So: **translate** = language conversion; **rewrite** = script for narration; **TTS** = audio from that script.

## Whole flow (end-to-end)

This section walks the **entire journey** from story creation to audio on the app. Default config assumed: `AUDIO_AFTER_APPROVAL=true`.

---

### Phase 1: Create or edit the story (Library / Edit)

- You create a new story or open an existing one in the admin (Stories → New or Edit).
- You write or paste **source-language** content (e.g. Tamil). You can also use **Regenerate with prompt** here:
  - **Regenerate with prompt** (button on edit page): Calls `POST /stories/{id}/regenerate-with-prompt`. The backend (1) rewrites the story with the Tamixa conversion prompt (LLM, source language only), (2) if `generateForAllLanguages=true`, **translates** that result to en, hi, te, kn, ml via `translateIfNeeded`, and (3) **returns** transformed content + `translations` in the response. **Nothing is saved yet**; the form just shows the new content and other-language text. You can edit, then **Save** (draft) or **Submit for review**.
- When you click **Submit for review**, the request can include **translationContentEntries** (from Regenerate with prompt). The backend **saves** the library story and **writes** those other-language strings into `story_translation` rows. No pipeline runs when `AUDIO_AFTER_APPROVAL=true`.
**Summary Phase 1:** Story content (and optionally other-language text) is **saved**. Story status becomes PUBLISHED and it appears in **Story for review**. No audio, no pipeline.

---

### Phase 2: Story for review → Approve or Reject

- In **Story for review** you see stories that were submitted. You **Approve** or **Reject**.
- **Approve**: Sets `narration_approved_at`; story is “content approved for delivery.” It does **not** run the pipeline (no translate/rewrite/TTS here).
- **Reject**: Sends the story back (e.g. CHANGES_REQUESTED); author can edit and resubmit.

**Summary Phase 2:** Only approval state changes. No pipeline, no audio.

---

### Phase 3: Narration (Story to Speech) → Generate audio

- **Narration (Story to Speech)** lists stories that are **content-approved** (have `narration_approved_at`).
- You click **Generate audio** (first time) or **Regenerate audio** (replace existing). That triggers the **full pipeline** for that story:
  - For each supported language (source + targets), the pipeline runs **translate** (if needed, or reuses existing translation content) → **rewrite** (conversational script via LLM) → **TTS** (audio to S3, `story_narration_audio`).
- Progress is shown via pipeline-status polling; when all languages are done, the story is READY and preview/play works.

**Summary Phase 3:** This is when **translate + rewrite + TTS** run (or only rewrite + TTS if translation already exists). Audio is created only after approval.

---

### How the pieces fit together

| Action | What runs | What gets saved |
|--------|-----------|------------------|
| **Regenerate with prompt** (edit page) | LLM convert (source) + optional translate (other langs) | Nothing; returns content + `translations` to the form. |
| **Submit for review** | Nothing | Library story + optional translation rows from request. |
| **Approve** (Story for review) | Nothing | Only `narration_approved_at`. |
| **Generate audio** / **Regenerate audio** (Story to Speech) | Full pipeline: translate (if needed) + rewrite + TTS per language | Translation rows (if created), narration script, audio in S3, `story_narration_audio`, story status READY. |

So: **content and optional other-language text** are decided in Phase 1 (edit + submit). **Audio** is generated only in Phase 3, after approval.

## Three-step flow: creation → approval → audio (Story to Speech)

When **audio-after-approval** is enabled (`AUDIO_AFTER_APPROVAL=true`, the default), the flow is:

1. **Create/edit story** → **Submit for review** → story goes to **Story for review** (no pipeline runs: no translate, rewrite, or TTS).
2. **Story for review** → **Approve** (or Reject). Approval sets `narration_approved_at` (content approved for delivery).
3. **Narration (Story to Speech)** → list shows approved stories → **Generate audio** (first time) or **Regenerate audio** (replace existing) runs the pipeline. From the user’s perspective this step runs **only TTS**; under the hood the pipeline still does translate + rewrite + TTS per language when needed (e.g. first time or when content was missing). Audio is generated only after approval.

Backend: `app.translation-pipeline.audio-after-approval` controls this. When `true`, Submit for review does not trigger the pipeline. The **Story to Speech** list is served by `GET /admin/stories/to-speech`; **Generate audio** calls `POST /admin/stories/{id}/trigger-pipeline`; **Regenerate audio** calls `POST /admin/stories/{id}/regenerate-narration` (clears old audio, then runs pipeline).

## Regenerate audio flow (end-to-end)

When the admin clicks **Regenerate audio** on the Story to Speech page (story already has audio, `overallStatus === "READY_FOR_REVIEW"`), the following happens:

1. **Admin UI** (`admin/src/app/(dashboard)/dashboard/stories/to-speech/page.tsx`): `hasAudio(row.id)` is true → calls `api.admin.regenerateLibraryStoryNarration(storyId)` or with `{ languages: ["en","hi","te"] }` for selective regenerate (e.g. truncation-flagged languages).
2. **Admin API** (`admin/src/lib/api.ts`): `POST /api/v1/admin/stories/{id}/regenerate-narration` with optional body `{ languages: ["en","hi","te"] }`. When languages provided, only those are invalidated and TTS-only; when omitted, all languages.
3. **Backend controller** (`AdminController.kt`): `POST /stories/{id}/regenerate-narration` → returns **200 immediately** with `"Regenerate narration triggered for curated story $id"`, and submits work to `triggerPipelineExecutor` (background thread).
4. **Backend service** (`StoryProcessingService.regenerateNarration(masterStoryId)`):
   - Loads master story and translations; if no translations, logs and returns.
   - **Invalidate**: `storyLibraryService.invalidateNarrationAudioForLanguages(masterStoryId, supportedLanguages)`:
     - For each supported language that has a translation: deletes `story_narration_audio` rows, deletes S3 objects (keys from `audioUrl`), sets translation status to `PENDING`, clears `progressTracker` for this story. If source language is in the set, clears `library_story.audio_file_url`.
     - Uses `REQUIRES_NEW` transaction so DB/S3 changes commit before pipeline runs; pipeline-status API then sees `PENDING` and no audio.
   - **Run pipeline**: `processLanguagesInternal(masterStoryId, supportedLanguages)` (same as trigger-pipeline path):
     - Sets story status to `PROCESSING`; creates processing job; for each language (in parallel by default) calls `processLanguage(masterStoryId, language, ...)`.
5. **Per-language pipeline** (`processLanguage`):
   - Translation row exists (from previous run); narration rows were deleted, so `hasReadyAudio` is false → no idempotent skip.
   - **Translation step** (`runTranslationStep`): Existing translation with valid content → reuses content, sets status to `REWRITING`, returns content (no new LLM translate call).
   - **Rewrite step**: `runRewriteStep` → conversational rewrite (LLM).
   - **TTS step**: Build SSML (or use emotion-tagged path), call `ttsService.synthesize`, then `audioStorage.uploadNarrationAudio` (S3); save `StoryNarrationAudio` with `READY`, update `library_story.audio_file_url` for source language; persist scenes/segments and narration script.
6. **After all languages**: Story status set to `READY` if all languages have audio, else `PUBLISHED`. Progress tracker is cleared at the start (in `invalidateNarrationAudioForLanguages`); while the pipeline runs, each language sets `progressTracker.setProcessing(masterStoryId, language)`. When the next poll sees all languages COMPLETED and no DB in-progress, `getPipelineStatusByStoryId` clears any stale tracker entry.
7. **Admin progress**: Admin polls `GET /api/v1/admin/pipeline/status-batch?ids=...`. `StoryLibraryService.getPipelineStatusByStoryId` returns `overallStatus` (e.g. `TRANSLATING_LANGUAGES`, `TTS_PROCESSING`, `READY_FOR_REVIEW`), `progress` (0–100), and per-language status; `PipelineProgressTracker.getProcessingLanguage(masterStoryId)` drives the “Generating X audio…” label. When all languages are COMPLETED, `overallStatus` becomes `READY_FOR_REVIEW` and preview audio will be available.

**Difference from Generate audio (trigger-pipeline):** Trigger-pipeline calls `processSync(id)`, which only processes languages that don’t already have audio (or are retriable). Regenerate explicitly invalidates all narration for supported languages first, then runs the full pipeline for every supported language so audio is replaced.

## Bulk story generation prompt

Bulk generation (admin "Bulk story generator") uses a single user prompt (no separate system message). The prompt is built by `StoryLibraryService.buildBulkGenerationPrompt(language, category, index)`.

- **Language**: The prompt includes an explicit **output language instruction** so the model writes in the requested language (e.g. Tamil), not a default. For every language, a style hint is added: use correct grammar, natural word order, and natural spoken style. To avoid titles (and theme/moral) appearing in Hindi when another language was requested, the prompt explicitly states: "Do not use Hindi for title, theme, or moral unless the output language is Hindi" and that all JSON string fields must be in the specified output language.
- **Category** and **Combined situation** are injected from the request and a fixed list of situations (village festival, school event, farm morning, etc.).
- **Security & compliance (Indian standards)**: The prompt includes a dedicated section so generated content aligns with Indian norms for children's content:
  - **Child safety**: No violence, gore, horror, abuse, fear-inducing scenes, or harmful material; no dangerous or easily imitable behaviour.
  - **Indian compliance**: No disparagement of religion, community, region, or language; no political or hateful/discriminatory content.
  - **Positive values only**: Respect for elders, friendship, honesty, sharing, inclusivity; peaceful conflict resolution; no bullying or negative stereotyping.
  - **Age-appropriate**: Suitable for children (including under 12); no mature themes, romance, or content unsuitable for general family audience in India.
- **Story quality & ending**: Mild conflict with peaceful resolution; positive, satisfying ending; moral as one short sentence; title catchy and specific (not generic).
- **Narration structure**: Short blocks (1–3 sentences), spoken transitions ("Once upon a time…", "One day…"), dialogue simple and sparse; every sentence natural when read aloud.
- **Educational perspective**: Build learning into the plot (no lecturing), include SEL (characters name feelings), weave in 2–4 wonder words used clearly and at least twice, add an inference-friendly moment, and ensure one clear problem with an earned resolution (age band approx. 5–7).
- **Do not include**: Explicit banned list—violence, weapons, death, war, politics, religious conflict, drugs, alcohol, self-harm, advertising/brand names, real celebrities or sensitive real places, adult themes. Purely fictional and uplifting.
- **Structure**: Same Tamixa style (family-friendly, 1500–1750 words, JSON with title, category, theme, story_text, moral, estimated_duration_seconds). Cultural context (Indian village/town, kindness/friendship) and JSON output format apply.
- **Flow**: One prompt per story; `openAI.generateStory(prompt, 2048)` is called in a loop. Stored prompt is saved on the library story as `convertPromptUsed` for audit.
- **estimated_duration_seconds**: When the model returns `estimated_duration_seconds` (or legacy `estimated_duration` like "10 min"), it is parsed and used as `readingTimeMinutes` for the library story (clamped 0.5–30 min).
- **Story review before deliver**: Before saving each story to the system, the generated `story_text` is run through **content moderation** (`StoryModerationService.moderateBeforeSave`). The same multi-layer moderation used for user-generated stories applies: OpenAI Moderation API, keyword blocklist, red-flag pattern detection, age-based vocabulary. If moderation rejects the content, that story is not saved and is reported in the bulk result as failed with error `content_moderation: ...`. Only stories that pass moderation are persisted and (if publish) submitted to the pipeline.

### Optional additions (if you extend the prompt later)

You can further refine the bulk prompt by adding:

- **Emotion mode**: e.g. "Calm/soothing for bedtime" or "Gently adventurous for daytime" (align with single-story emotionMode).
- **Vocabulary band**: e.g. "Use very simple words for ages 5–7" or "Slightly richer vocabulary for 8–12" (could be driven by a future age parameter).
- **Character diversity**: e.g. "Include a mix of characters (names from different regions); avoid stereotyping."
- **Specific no-go topics**: If you have a blocklist (e.g. certain animals, situations), add one line: "Do not use: [X, Y, Z]."
- **Moral scope**: e.g. "Moral must be a single sentence and one of: kindness, honesty, sharing, courage, respect, friendship."
- **Opening/closing phrase**: e.g. "Start with a classic opener in the output language; end with a short closing line (e.g. 'And that is how…')."
