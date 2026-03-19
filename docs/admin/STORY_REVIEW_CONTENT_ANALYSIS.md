# Why story content in the review screen can look “dry” or “flat”

## Summary

**The content you see in Story for review is the narrated (rewritten) version only after the pipeline has completed for that story and language.** If the pipeline has not run or has not finished, the screen shows the **original story text** (or translated-but-not-rewritten text), which can feel dry or flat because it has not yet been run through the **narration rewrite** step.

---

## How the pipeline affects content

1. **Submit for review**  
   Story is saved with status PUBLISHED. The pipeline is triggered (translate → rewrite → TTS).

2. **Pipeline steps (per language)**  
   - **Translate** – Source story is translated (for non-Tamil languages).  
   - **Rewrite** – Text is rewritten for narration (conversational, TTS-friendly). This is the step that turns “flat” text into the narrated version.  
   - **TTS** – Audio is generated from the rewritten script.

3. **What gets stored**  
   After the pipeline completes for a language, the **rewritten script** is stored in the translation row. The API returns this as `content` for that language. The review screen shows `content` (and, when present, `narratedContent`).

4. **If the pipeline has not completed**  
   - There may be no translation row yet, or the translation still has the **original** (or only translated) text.  
   - So in the review screen you see the **pre-rewrite** text, which has not been through the narration prompt and can feel dry/flat.

---

## Story 33 (and similar cases)

If **story 33** was disabled on both tables, the pipeline for that story had not completed (or had not run). So:

- The **Story for review** row was disabled (row is disabled until the pipeline is complete).  
- The **Story library** Run pipeline button was disabled (before we changed it to enable when the pipeline is not complete).

When you open the translation/language modal for that story, the API returns whatever is stored today:

- If the pipeline **has not completed** for that language → you see **original** (or translated-only) text → it can look **dry/flat**.  
- Once the pipeline **completes** (translate → rewrite → TTS) → the stored content is updated to the **rewritten narration script** → the same modal would then show the **narrated** version.

So **story 33 is not narrated based on the prompt yet** for the same reason it was disabled: the pipeline had not successfully completed. The “dry/flat” content is the pre-pipeline text.

---

## What to do

1. **Run the pipeline** for story 33 from **Story library** (▶ Run pipeline).  
2. Wait for the pipeline to finish (refresh to see status; when the language shows COMPLETED, the rewrite step has run).  
3. Open **Story for review** again and edit that language – you should now see the **rewritten, narration-style** content.  
4. If the pipeline fails (e.g. FAILED in Pipeline column), use **Run pipeline** again to retry, or fix the underlying error (e.g. translation/TTS config) and retry.

---

## Technical note (backend)

- **StoryProcessingService** (used by trigger-pipeline and approve-narration) runs: translate → **rewrite** (via `rewriteService.rewrite()`) → TTS, and saves the rewritten script into **translation.content**.  
- The review screen loads content via `GET /admin/stories/:id` with a language; the API returns `content` = that translation’s content (or master content if no translation). So after the pipeline completes, `content` is the narrated script.  
- `narratedContent` in the API comes from the `story_narration_scripts` table, which is populated by a different path (Orchestrator). The admin review screen prefers `narratedContent` when present, then falls back to `content`. For the pipeline triggered from the admin (processSync), the rewritten text is in **translation.content**, so the review screen will show the narrated version once the pipeline has completed for that language.
