# Story pipeline – process flow

This document describes **when the narration pipeline runs** and **what each admin action does**. Behavior depends on **AUDIO_AFTER_APPROVAL** (default `true`).

- **AUDIO_AFTER_APPROVAL=true** (recommended): Submit saves content only; Approve sets approval only; **Generate audio** in Story to Speech triggers the pipeline.
- **AUDIO_AFTER_APPROVAL=false**: Submit triggers full pipeline; Approve only marks approved.

See [STORY_AUDIO_AVATAR_FLOW.md](STORY_AUDIO_AVATAR_FLOW.md) for the full config matrix and end-to-end flow.

---

## Pipeline in one sentence

The **pipeline** = **translate** (per language) → **rewrite for TTS** → **generate audio** → story becomes playable. It runs in the background and can take several minutes.

---

## Flow overview

| Step | Action | Pipeline (AUDIO_AFTER_APPROVAL=false) | Pipeline (AUDIO_AFTER_APPROVAL=true, default) |
|------|--------|--------------------------------------|----------------------------------------------|
| 1 | Create/edit story | — | — |
| 2 | Submit for review | **Runs** (translate + TTS) | **No** – saves content only |
| 3 | Story for review | Content populates as pipeline completes | Content from Submit |
| 4 | Approve for delivery | **No** – marks approved | **No** – marks approved |
| 5 | Story to Speech → Generate audio | Manual if needed | **Runs** pipeline |
| 6 | Pipeline fails | Retry / Regenerate from Story library | Same |

---

## Intended process flow

### First time (new story)

1. **Content creator** creates/edits the story in **Story library** (Edit story). No pipeline runs.
2. **Submit for review** → status becomes PUBLISHED; story is sent to **Story for review**. **Pipeline runs** (translate + TTS for all languages).
3. Story appears in **Story for review** queue; content for en, hi, te, kn, ml populates as the pipeline completes (refresh to see updates).
4. In **Story for review**, reviewers verify content per language. Then **Approve for delivery**.
5. **Approve for delivery** → marks story approved; no pipeline run (content already exists). Story becomes visible on the app.

### Second time (content creator changes the story)

1. **Content creator** edits the story again in **Story library** and **Submit for review**.
2. **Submit for review** → pipeline runs again (resets and regenerates translate + TTS). Story goes to **Story for review**.
3. Reviewers edit languages if needed; **Approve for delivery** → marks approved. Story goes live (no pipeline run).

### Failure case

- If the pipeline **fails** (e.g. translation or TTS error), use **Run pipeline** (▶) or **Regenerate audio** (↻) in the **Story library** table to retry.

---

## When does the pipeline start?

| Where you are | What you do | Does pipeline run? |
|---------------|-------------|--------------------|
| **Story library** → Create or Edit story | Submit for review | **Yes** – pipeline runs (translate + TTS for all languages). |
| **Story for review** | **Approve for delivery** | **No** – only marks approved; content already exists from Submit for review. |
| **Story library** → **Run pipeline** (▶) | Click **Run pipeline** on a story | **Yes** – manual start. Use when pipeline failed or story needs reprocessing. |
| **Story library** → **Regenerate audio** (↻) | Click **Regenerate audio**, choose languages | **Yes** – clears audio for chosen languages and runs pipeline again. |

---

## Actions quick reference

| Action | Where | Effect |
|--------|--------|--------|
| **Submit for review** | Story library → Edit story | Sets status to PUBLISHED; story goes to Story for review. **Pipeline runs** (content generated for all languages). |
| **Approve for delivery** | Story for review | Marks story approved (no pipeline); story becomes visible on the app. |
| **Reject** | Story for review | Removes story from queue (status → REJECTED). Creator can edit and submit again from Story library. |
| **Run pipeline** (▶) | Story library table | Manual start. Use when pipeline failed or story needs reprocessing. |
| **Regenerate audio** (↻) | Story library table | Clears audio for selected languages and runs pipeline. |

---

## Summary

- **Pipeline runs** on Submit for review (content generated for all languages).
- **Approve for delivery** does *not* trigger the pipeline; it only marks the story approved.
- **On pipeline failure**, use **Run pipeline** or **Regenerate audio** from Story library to retry.
- While pipeline is running, a banner is shown and Submit for review is disabled for that story.
- Config: `PIPELINE_ON_SUBMIT_ONLY=true` (default) means pipeline runs only when Submit for review is used; when false, publish/update also triggers (legacy flow).
