# Voice cloning: why synthesis ran every time and how it works

## Summary (AI Voice Expert view)

**Why synthesis was generated every time**

- The pipeline treated **cloned** voices differently from default/calm: it **never** reused existing READY audio. For every preview or on-demand request it ran full flow: format → validate → TTS (XTTS/ElevenLabs) → upload. That was a deliberate safeguard so we never served an old **stub** (e.g. `tamixa.mp3`) that had been saved when XTTS failed or wasn’t configured.
- So each “play cloned voice” effectively triggered a **full regeneration**, which felt like “synthesis every time” and “not processing properly” (slow, repeated work, possible duplicate requests).

**Why it looked like “not processing properly”**

1. **No cache for cloned** – Every run re-did formatting, safety checks, and TTS instead of returning existing READY audio.
2. **Duplicate requests** – Two calls (e.g. double fetch, Strict Mode) both saw “no READY” and both started generation; logs showed repeated chunks and two `POST /synthesize` 200 OK.
3. **Chunking noise** – One XTTS request can have many chunks; Coqui logged “Text splitted to sentences” per chunk, so logs looked like the same block repeating.
4. **Format mismatch (fixed earlier)** – WAV was stored as `.mp3` and served as `audio/mpeg`, so playback sounded like hiss/“air”; we fixed by transcoding WAV→MP3 before upload.

## Current behavior (after fix)

- **Idempotent per (translation, voiceProfile)**  
  If `story_narration_audio` already has a row with `status = READY` for that translation and voice (including `cloned:{id}`), the orchestrator **skips** TTS and upload and reuses the existing URL. Synthesis is **not** regenerated every time.
- **When we do run synthesis**
  - **Default/calm:** Only when there is no READY row.
  - **Cloned:** Same rule: only when there is no READY row. We still reject tiny outputs (< 80k bytes) as stub and save FAILED, and we transcode WAV→MP3 before upload, so READY is trusted.
- **On-demand path** – `AudioStreamService.getCuratedNarrationStoragePath` / `getCuratedNarrationStreamUrl` only call `generateClonedVoiceOnDemand` when there is **no** READY audio. So first preview triggers one full run; subsequent previews reuse the same READY file.

## Flow (cloned voice)

1. User requests stream/preview for story S, language L, `voiceProfile=cloned:V`.
2. Backend looks up `story_narration_audio` for (translation(S,L), `cloned:V`).
3. **If READY exists** → return that audio URL/path; **no** TTS call, no XTTS.
4. **If no READY** → `generateClonedVoiceOnDemand` → `StoryProcessingOrchestrator.process` with `voiceProfiles = [cloned:V]`:
   - Format script, validate, build SSML.
   - For `cloned:V`: if READY exists → skip (idempotent); else call TTS (ClonedVoiceStrategy → XTTS or ElevenLabs), reject if &lt; 80k, transcode WAV→MP3 if needed, upload, save READY.
5. Next request for same (S, L, cloned:V) hits step 3 and reuses READY.

## Where voice cloning is triggered

- **Admin (Voice & Avatar Studio):** When an admin selects a story and parent in Voice & Avatar Studio, the voice cloning process can start there: upload a voice file to create a clone for that parent, then use the cloned voice for the selected story (preview with cloned voice, then play with avatar).
- **End users (mobile first; web TBD):** On the **stories screen**, the user can choose to listen to a story with their cloned voice. If they do not have a voice profile yet, they are offered the option to **upload a voice file**; that starts the voice cloning process. Once the clone is ready, they can listen with their voice.

## Recommendations

- **Avoid duplicate triggers** – Ensure preview/play only issues one request (no double mount, no duplicate fetch).
- **Observe logs** – `XTTS synthesis completed` vs `did not complete` / `stub`; `Narration READY for translationId=… voice=cloned:…` when we persist.
- **Re-generate when needed** – If you need to force new audio (e.g. new reference, fixed TTS), clear or mark the existing READY row for that (translation, voice) so the next request runs synthesis again.
