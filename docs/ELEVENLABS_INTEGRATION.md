# ElevenLabs integration – working flow

This document describes how cloned-voice narration uses ElevenLabs and how to get it working.

---

## Flow (ClonedVoiceStrategy)

When a user requests narration with **cloned voice** (`voiceProfile=cloned:{profileId}`):

1. **Existing ElevenLabs voice**  
   If the voice profile already has `elevenlabs_voice_id` and ElevenLabs is enabled:
   - Call `ElevenLabs.synthesize(text, voiceId, language)`.
   - Return MP3 bytes.

2. **Create voice from reference (first use)**  
   If the profile has `reference_audio_path` (e.g. `voices/18/file.mp3`) and ElevenLabs + S3 reference storage are available:
   - Load reference audio bytes from S3 via `VoiceReferenceStoragePort.getReferenceAudio(path)`.
   - Call `ElevenLabs.addVoice(refBytes, "reference.mp3", "clone-{profileId}")` to create a voice.
   - Save the returned `voice_id` on the profile (`voiceRepository.save(profile.copy(elevenlabsVoiceId = newVoiceId))`).
   - Call `ElevenLabs.synthesize(text, newVoiceId, language)` and return MP3 bytes.

3. **XTTS fallback**  
   If ElevenLabs is not configured, addVoice failed (e.g. 401), or reference could not be loaded:
   - If self-hosted XTTS is configured (`app.voice-cloning.xtts-base-url`), call `SelfHosted.synthesize(text, referencePath, language)` (WAV; backend transcodes to MP3 on upload).

---

## Requirements for ElevenLabs to be used

| Requirement | Config / Env | Notes |
|-------------|----------------|------|
| Voice cloning enabled | `VOICE_CLONING_ENABLED=true` or `app.voice-cloning.enabled=true` | Creates `ElevenLabsVoiceCloningAdapter` bean |
| API key | `ELEVENLABS_API_KEY=xi_...` | From [elevenlabs.io](https://elevenlabs.io) API keys. Must match YAML key `app.voice-cloning.eleven-labs-api-key` (kebab-case). |
| Reference storage (for “create from reference”) | `app.storage.type=s3` | So `VoiceReferenceStoragePort` (S3) can read `voices/{parentId}/{file}` for addVoice |
| Profile has reference | DB: `voice_profiles.reference_audio_path` set | Set when parent uploads a voice sample; path like `voices/18/filename.mp3` |

---

## Env checklist (.env)

```bash
# Enable ElevenLabs for cloned voice
VOICE_CLONING_ENABLED=true

# Get key from https://elevenlabs.io → Profile → API Key
ELEVENLABS_API_KEY=xi_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# S3 required so backend can read reference audio for addVoice (and store narration)
# If you already use S3 for app, no extra config.
```

Restart the backend after changing env.

---

## Logs to confirm working flow

- **Startup:**  
  `ElevenLabs integration enabled: baseUrl=..., apiKey=set (***xxxx). Cloned narration will use ElevenLabs when profile has reference_audio_path and S3 reference storage is configured.`

- **First use (create from reference):**  
  `Cloned voice: creating ElevenLabs voice from reference profileId=... refBytes=... lang=...`  
  `Cloned voice: ElevenLabs voice created profileId=... voiceId=...`  
  `Cloned voice: ElevenLabs synthesis completed profileId=... lang=... bytes=...`

- **Later use (existing voice_id):**  
  `Cloned voice: ElevenLabs synthesis completed profileId=... lang=... bytes=...`

- **Fallback to XTTS:**  
  `Cloned voice: ElevenLabs addVoice failed for profileId=... (check API key), falling back to XTTS if configured`  
  or  
  `Cloned voice: could not load reference audio for profileId=... path=..., falling back to XTTS if configured`

---

## Dev endpoint (profile=dev)

`GET /api/v1/dev/voice-cloning-status` returns:

- `voiceCloningEnabled`, `elevenLabsApiKeySet`, `elevenLabsBeanPresent`, `elevenLabsReady`
- `voiceReferenceStoragePresent` (S3 adapter for reference audio)
- `xttsBaseUrlSet`
- Short `flow` and `checklist` for troubleshooting

Use this to verify config and beans without triggering narration.

---

## How to test

### 1. Verify config (before testing narration)

**Option A – Dev endpoint** (backend must run with `spring.profiles.active=dev`):

```bash
curl -s http://localhost:8080/api/v1/dev/voice-cloning-status | jq
```

Check: `elevenLabsReady: true`, `voiceReferenceStoragePresent: true` (if using S3). If not, fix env (see Env checklist) and restart.

**Option B – Startup logs**

After starting the backend, look for:

```
ElevenLabs integration enabled: baseUrl=https://api.elevenlabs.io, apiKey=set (***xxxx)
```

If you see `apiKey=missing`, set `ELEVENLABS_API_KEY` in `.env` and restart.

---

### 2. Prerequisites

- A **parent** that has uploaded a **voice sample** (so a voice profile exists with `reference_audio_path` in DB).
- A **curated story** with at least one translation (e.g. Tamil or English).
- **Admin** (or an API token with admin scope) to call the preview/stream endpoints.

---

### 3. Test via Admin UI

1. Log in to the **admin** dashboard.
2. Open **Curated stories** and pick a story.
3. Use the **preview / play** with **cloned voice**:
   - If the UI has a voice selector, choose the parent’s cloned voice (e.g. “Cloned voice” or “Parent’s voice”).
   - Or open the story detail/edit and use “Preview audio” with **language** (e.g. Tamil) and **cloned voice** (and parent ID if the UI asks).
4. First play: backend creates the ElevenLabs voice from the reference, then synthesizes. Later plays: reuse the same voice (faster).
5. Confirm you hear the **story in the cloned voice** (not the stub or default TTS).

---

### 4. Test via curl (preview audio with cloned voice)

Replace `STORY_ID`, `PARENT_ID`, `VOICE_PROFILE_ID`, and `ADMIN_TOKEN` (Bearer token or session cookie if your auth uses it).

**Get preview-audio URL (returns audio bytes):**

```bash
# With Bearer token (JWT)
curl -s -o preview.mp3 \
  -H "Authorization: Bearer YOUR_ADMIN_JWT" \
  "http://localhost:8080/api/v1/admin/curated-stories/STORY_ID/preview-audio?language=ta&voiceProfile=cloned:VOICE_PROFILE_ID&parentId=PARENT_ID"

# Play the file (macOS)
afplay preview.mp3
```

Example: story ID `31`, parent ID `18`, voice profile ID `1`:

```bash
curl -s -o preview.mp3 \
  -H "Authorization: Bearer YOUR_JWT" \
  "http://localhost:8080/api/v1/admin/curated-stories/31/preview-audio?language=ta&voiceProfile=cloned:1&parentId=18"
afplay preview.mp3
```

---

### 5. Test XTTS fallback (optional)

1. Turn off ElevenLabs: set `VOICE_CLONING_ENABLED=false` (or remove `ELEVENLABS_API_KEY`), restart backend.
2. Start the XTTS service: `cd xtts-service && source venv-xtts/bin/activate && uvicorn main:app --host 0.0.0.0 --port 9000`.
3. Trigger the same preview (same story + cloned voice + parent). Backend should use XTTS; XTTS terminal should show `SYNTHESIS_COMPLETED`.
4. Restore ElevenLabs env and restart to switch back.

---

### 6. What to look for in logs

| Log message | Meaning |
|-------------|--------|
| `Cloned voice: creating ElevenLabs voice from reference profileId=...` | First-time: creating voice from S3 reference. |
| `Cloned voice: ElevenLabs voice created profileId=... voiceId=...` | Voice created; `voice_id` saved on profile. |
| `Cloned voice: ElevenLabs synthesis completed profileId=... lang=... bytes=...` | Narration generated with ElevenLabs (working flow). |
| `Cloned voice: ElevenLabs addVoice failed ... falling back to XTTS` | API key or quota issue; XTTS used if configured. |
| `Cloned voice: using XTTS profileId=...` | Fallback path: no ElevenLabs or creation failed. |

---

## Troubleshooting

| Symptom | Check |
|--------|--------|
| 401 Unauthorized from ElevenLabs | `ELEVENLABS_API_KEY` valid and correctly bound to `app.voice-cloning.eleven-labs-api-key` (see [application.yml](../backend/src/main/resources/application.yml) and adapter `@Value`). |
| “ElevenLabs addVoice skipped: API key is blank” | Env not loaded or key name mismatch (use `eleven-labs-api-key` in YAML). |
| “could not load reference audio” | S3 configured; path is `voices/{parentId}/...`; object exists in bucket. |
| Always uses XTTS, never ElevenLabs | `VOICE_CLONING_ENABLED=true`, key set, and (for create-from-reference) S3 + `VoiceReferenceStoragePort` present. Call `/dev/voice-cloning-status` to confirm. |

---

## Indian languages

ElevenLabs multilingual models support **Tamil** and other Indian languages natively. The adapter maps language codes (e.g. `ta`, `te`, `ml`, `hi`, `bn`) in `mapLanguageToElevenLabs`. When ElevenLabs is used, the story is synthesized in the requested language without mapping to Hindi as with XTTS.
