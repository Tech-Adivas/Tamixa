# Avatar Video (Talking-Head) Feature

**Goal:** The avatar tells the story using the **cloned voice** (parent’s voice). Premium parents upload an avatar image; the app animates it into a talking-head video driven by the cloned-voice narration for each story.

**UI flow:** For step-by-step implementation of “uploaded avatar (favorite person photo) tells stories” in Admin and Mobile UI, see [AVATAR_TELLS_STORIES_UI.md](AVATAR_TELLS_STORIES_UI.md).

**Avatar video provider:** **HeyGen is primary.** Use `AVATAR_VIDEO_PROVIDER=heygen` and `HEYGEN_API_KEY` for avatar video. **Replicate (SadTalker), D-ID, and Gooey are fallbacks** when HeyGen is not configured; the app and logs state clearly that the fallback is used (primary is HeyGen).

## How the Replicate token is used (lip-sync API)

This application talks to **Replicate’s Predictions API** (their lip-sync / talking-head model “SadTalker”) only from the backend. The token is never sent to the browser or mobile app.

1. **Where you set the token**  
   In the **backend** `.env` (project root, same folder as `build.gradle.kts`):
   ```bash
   REPLICATE_API_TOKEN=r8_xxxxxxxxxxxx
   ```
   Get the value from: [Replicate → Account → API tokens](https://replicate.com/account/api-tokens).

2. **How the backend loads it**  
   - `application.yml` has: `app.avatar-video.replicate-api-token: ${REPLICATE_API_TOKEN:}`  
   - When you run the backend (e.g. `./gradlew :backend:bootRun`), Gradle loads `.env` and sets `REPLICATE_API_TOKEN` in the process environment.  
   - Spring binds that to `app.avatar-video.replicate-api-token`.  
   - If you run the backend from an IDE, the IDE usually does **not** load `.env`; add `REPLICATE_API_TOKEN` to the run configuration’s environment variables.

3. **How the app talks to Replicate’s lip-sync API**  
   - When avatar video is needed, **AvatarVideoService** calls **ReplicateSadTalkerClient**.  
   - The client sends **HTTPS** requests to Replicate:
     - **Create job:** `POST https://api.replicate.com/v1/predictions`  
       - Headers: `Authorization: Bearer <REPLICATE_API_TOKEN>`, `Content-Type: application/json`  
       - Body: `version` (SadTalker model), `input.source_image` (avatar image URL), `input.driven_audio` (narration audio URL).  
     - **Check status:** `GET https://api.replicate.com/v1/predictions/{id}` (same `Authorization` header).  
   - So the **only** place your token is used is in that `Authorization` header to Replicate’s API. The app does not call any other “lip-sync” URL; Replicate’s API is the lip-sync service.

4. **How to verify the app is using Replicate**  
   - **Admin:** In **Voice & Avatar Studio**, when you request “Play with avatar”, the stream-url response includes `avatarVideoProvider: "sadtalker"` when the Replicate client is configured. If you see that, the backend has the Replicate client and is the one that will call Replicate when generating a video.  
   - **Backend logs:** When a video is actually created you’ll see:  
     `Replicate prediction created id=...`  
     and later  
     `Avatar video READY storyId=... parentId=... path=...`  
   - **If you don’t see `avatarVideoProvider`** or you see “No avatar video provider”, then either `AVATAR_VIDEO_PROVIDER` is not `sadtalker` (or has extra characters), or `REPLICATE_API_TOKEN` is empty, or the backend was started without loading `.env` (e.g. from IDE without env vars).

## End-to-end integration (generated + frontend)

| Layer | What happens |
|-------|----------------|
| **Backend** | `AvatarVideoService` uses **HeyGen as primary**; Replicate, D-ID, Gooey are **fallbacks** when HeyGen is not configured (logs and `avatarVideoProvider` indicate "fallback; primary is HeyGen"). Uses cloned-voice narration audio to drive the avatar; stores video in S3; `getAvatarVideoUrl` returns signed URL when READY. |
| **Admin (Voice & Avatar Studio)** | `GET /api/v1/admin/curated-stories/:id/stream-url?language=&voiceProfile=cloned:X&parentId=` returns `CuratedStoryStreamUrlResponse`. Page shows video when `avatarVideoUrl` exists; uses video’s native audio when present (best lip-sync); fallback: muted video + separate narration. Regenerate and Try again call delete then stream-url to start a new generation. Polling uses the same endpoint for status. |
| **Mobile** | `GET /api/v1/stories/:id/stream-url` (authenticated parent) returns `streamUrl`, `avatarUrl`, `avatarVideoUrl`. When `avatarVideoUrl` is set and subscription is entitled, `TamixaNavHost` uses `AvatarVideoSurface` (ExoPlayer/AVPlayer); video includes embedded audio. Fallback: static avatar + audio. |
| **Web** | `Stories` page receives `avatarVideoUrl` from stream-url; `StoryAudioPlayer` shows video when present and uses it for playback (video includes audio). |

All frontends prefer the **video’s built-in audio** when available so lip-sync stays correct; no separate narration track is played over the video in that case.

## Default TTS, voice cloning, and avatar

- **Default narration:** **Google Cloud TTS** is the default (`NARRATION_TTS_PROVIDER=google`). Set `GOOGLE_CLOUD_TTS_API_KEY` (and optionally `NARRATION_GOOGLE_TTS_VOICE_NAME`, `NARRATION_GOOGLE_TTS_SPEED`) for standard story narration.
- **Cloned voice (narration):** When a parent uses a cloned voice profile (`cloned:{profileId}`), narration is synthesized with **HeyGen TTS** (if the profile has `heygen_voice_id` and `HEYGEN_API_KEY` is set), then **ElevenLabs**, then **XTTS** (self-hosted). To use HeyGen for cloned voice: create the voice in [HeyGen app](https://app.heygen.com/voices), then set it on the profile via **PATCH** `/api/v1/admin/parents/{parentId}/voice/{voiceProfileId}` with body `{ "heygenVoiceId": "your_heygen_voice_id" }`.
- **Avatar with cloned voice:** Use **HeyGen** for avatar video by setting `AVATAR_VIDEO_PROVIDER=heygen` and `HEYGEN_API_KEY`. The same key is used for both HeyGen avatar video and HeyGen voice TTS.
- **Where voice cloning starts:** Admin (Voice & Avatar Studio) when selecting story/parent; end users (stories screen, mobile first; web TBD) when they choose “listen with my voice” and can upload a voice file if they don’t have a clone.

## Flows verified (revisit checklist)

| Flow | Entry | Backend / API | Status |
|------|--------|----------------|--------|
| **Default TTS** | User plays story with default/calm voice | `NARRATION_TTS_PROVIDER=google` → `NeuralVoiceStrategy` → `GoogleCloudTtsClientAdapter`. Simulated only when provider=simulated. | OK |
| **Cloned voice (HeyGen)** | Profile has `heygen_voice_id`; user selects My voice | `ClonedVoiceStrategy` uses `HeyGenVoiceTtsPort` first when profile.heygenVoiceId set and `HEYGEN_API_KEY` set. | OK |
| **Cloned voice (ElevenLabs/XTTS)** | Profile has reference_audio_path or elevenlabs_voice_id | `ClonedVoiceStrategy` falls back to ElevenLabs then XTTS. | OK |
| **Available voices (incl. HeyGen)** | Mobile calls GET `/stories/:id/voices` | `StoryVoicesService.getAvailableVoices` includes `cloned:id` when profile has elevenlabsVoiceId, referenceAudioPath, or heygenVoiceId. | OK |
| **Stream URL (parent)** | GET `/stories/:id/stream-url?voiceProfile=cloned:X` | `resolveParentId(user)`; `getCuratedNarrationStreamUrl` → orchestration → `ClonedVoiceStrategy`. | OK |
| **Avatar video** | Premium parent; play with avatar | `getAvatarVideoUrl`; **HeyGen primary**; Replicate/D-ID/Gooey **fallback from HeyGen** when not configured; narration URL supports cloned voice. | OK |
| **Admin Voice & Avatar Studio** | Select story, parent, voice profile | GET admin stream-url with parentId, voiceProfile; PATCH for heygenVoiceId; delete avatar-video for regenerate. | OK |
| **Mobile: no clone → upload** | No cloned profile; voice menu on story | Menu shows "Upload voice to create my voice"; navigates to Voice Upload; after upload, new profile; next open story shows cloned option. | OK |

## Flow

1. **Upload**: Premium parent uploads avatar in Settings → Storytelling Avatar.
2. **First playback**: Parent opens a story. Backend checks for cached avatar video.
3. **Generation**: If no cache, backend creates a prediction (avatar image + story narration audio → video). Job runs async (~75 s for SadTalker).
4. **Fallback**: While video generates, mobile shows static avatar image + audio playback.
5. **Ready**: On next request (or refresh), `avatarVideoUrl` is returned. Mobile plays the video (contains both video and audio).
6. **Cache**: Generated videos stored in S3/GCS; subsequent plays use cached URL.

## Configuration

**Provider:** **HeyGen is primary** — set `AVATAR_VIDEO_PROVIDER=heygen` and `HEYGEN_API_KEY`. **Fallbacks** (when HeyGen is not configured): `sadtalker` (Replicate), `d-id`, `gooey`; the backend logs and admin UI state clearly that the fallback is in use (primary is HeyGen).

### Replicate SadTalker (fallback when HeyGen not configured)

In `.env`:

```
AVATAR_VIDEO_ENABLED=true
AVATAR_VIDEO_PROVIDER=sadtalker
REPLICATE_API_TOKEN=r8_...   # From https://replicate.com/account/api-tokens
```

Optional:

- `AVATAR_VIDEO_POLL_INTERVAL=20` – seconds between Replicate status polls (default 20)
- `AVATAR_VIDEO_MAX_POLL=30` – max poll attempts (default 30 → 10 min total; SadTalker can take 2–3 min on cold start)

### HeyGen (primary)

HeyGen is used for **avatar video** (talking head from photo + narration audio) and optionally for **cloned-voice TTS** (when a voice profile has `heygen_voice_id` set via admin PATCH). Same `HEYGEN_API_KEY` for both.
1. **Get an API key:** [HeyGen](https://app.heygen.com/) → **Settings** → **API Key** (or [developer docs](https://docs.heygen.com/)).
2. **Backend `.env`:**
   ```
   AVATAR_VIDEO_ENABLED=true
   AVATAR_VIDEO_PROVIDER=heygen
   HEYGEN_API_KEY=your_heygen_api_key_here
   ```
3. **Restart the backend.** In Admin → **Voice & Avatar Studio**, select parent (with avatar uploaded), story, and cloned voice; click **Play with avatar**. The backend uploads the avatar to HeyGen, creates a video with the cloned-voice narration URL, polls until done, then stores the result in S3.

See [HeyGen pricing](https://www.heygen.com/pricing) for limits and cost.

### D-ID (fallback when HeyGen not configured)

[D-ID Talks API](https://docs.d-id.com/docs/v2-photo-avatar-quickstart): image + **audio URL** (e.g. cloned-voice narration) → talking-head video. Good for trying avatar + cloned voice (e.g. **~2 min story**).

In `.env`:

```
AVATAR_VIDEO_ENABLED=true
AVATAR_VIDEO_PROVIDER=d-id
DID_API_KEY=...   # From https://studio.d-id.com/ or D-ID API keys
```

Optional: `DID_BASE_URL` (default `https://api.d-id.com`). The backend sends `source_url` (avatar image) and `script.audio_url` (narration); D-ID returns a talk `id`, we poll `GET /talks/{id}` until `status=done`, then use `result_url`. Typical processing 10–30 seconds.

### Gooey.AI (fallback when HeyGen not configured)

If you use Gooey as a fallback, set `AVATAR_VIDEO_PROVIDER=gooey`, `GOOEY_API_KEY`, and `GOOEY_RECIPE_ID`. **Primary is HeyGen**; Gooey is used only when HeyGen is not configured (logs and UI state "fallback; primary is HeyGen"). May not suit cloned-voice lip-sync as well as HeyGen.

## Backend

- **DB**: `story_avatar_video` table caches (story_id, story_source, parent_id, language, voice_profile) → storage_path
- **GooeyLipSyncClient**: When `provider=gooey`, calls [Gooey.AI API](https://api.gooey.ai) v2 run; creates job with `recipe_id` and input `image_url`, `audio_url`; polls until completed. Output: video URL from job output.
- **ReplicateSadTalkerClient**: When `provider=sadtalker`, calls [Replicate SadTalker API](https://replicate.com/cjwbw/sadtalker/api); creates prediction, polls until succeeded. Inputs: `source_image`, `driven_audio` (HTTP URLs), `use_enhancer`, `preprocess`, `still_mode`. Output: single URI string.
- **HeyGenAvatarVideoClient**: When `provider=heygen`, uploads talking photo and creates video
- **DidAvatarVideoClient**: When `provider=d-id`, calls [D-ID Talks API](https://docs.d-id.com/docs/v2-photo-avatar-quickstart): `POST /talks` with `source_url` + `script.type=audio` + `audio_url`; polls `GET /talks/{id}` until `status=done`; downloads from `result_url`
- **AvatarVideoService**: Orchestrates generation; triggers async job; returns signed URL when READY
- **Storage**: S3/GCS adapters for video files (avatar_videos/{storyId}_{parentId}_{lang}_{voice}.mp4)

## Mobile (Android & iOS)

- **API:** `GET /api/v1/stories/{id}/stream-url?language=...&voiceProfile=...&storySource=...` returns `streamUrl`, `avatarUrl`, `avatarVideoUrl`, `wordTimings`. Mobile uses the same endpoint as web; backend includes `avatarVideoUrl` when a cached talking-head video is READY for that story + parent + language + voice.
- **StoryApi.getStreamUrl:** Parses `StreamUrlResponse` and returns `StreamUrlResult.Url(url, avatarUrl, avatarVideoUrl, wordTimings)`.
- **TamixaNavHost (Audio Player):** Resolves `avatarVideoUrl` via `ApiConfig.resolveCoverUrl` → `streamAvatarVideoUrl`. When `streamAvatarVideoUrl` is non-blank, playback uses the avatar video URL (video contains both picture and narration audio). When blank (e.g. first play, or generation failed), playback uses `streamUrl` (audio only) and shows static `streamAvatarUrl` if present.
- **Android:** `rememberAvatarVideoController` → `rememberAvatarVideoExoPlayerController`. ExoPlayer loads the video URL (audio + video); `AvatarVideoSurface` shows the video via `PlayerView`. On error, `avatarVideoSurfaceFailed` is set and UI falls back to static avatar + audio controller.
- **iOS:** `rememberAvatarVideoController` → `createAvPlayerController` with AVPlayer; `AvatarVideoSurfaceIos` displays the layer. Same fallback on error.
- **AudioPlayerScreen:** When `storytellingAvatarVideoContent` is non-null, the screen shows the avatar video composable; otherwise it shows `storytellingAvatarUrl` (static image) or neither. Download button uses `streamAvatarVideoUrl` when in avatar-video mode (saves as `video/mp4`).

## Lip-sync and audio (root cause of mismatch)

**Root cause:** Replicate SadTalker’s output is a single video file that **includes the driven audio embedded** and aligned to the lip animation. If the UI mutes that video and plays a **separate** narration stream (same content, different element), two timelines can drift and the lips will not match the words.

**Fix:** When the video has a native audio track (Replicate’s output does), the admin and clients should **use the video’s built-in audio** (play the video unmuted). That gives a single timeline and correct lip-sync. Only when the video has no audio track (e.g. some edge case) should the app fall back to muted video + separate narration and sync logic.

## Cost

~$0.10 per video generation on Replicate. Cached per (story, parent, language, voice).

## VEED AI (Fabric 1.0) — avatar + cloned voice

**VEED** offers talking-head video via **Fabric 1.0**, hosted on **fal.ai**. It fits “avatar telling the story with cloned voice” because it takes **image + audio** and returns a lip-synced video.

### Fit for avatar + cloned voice

| Aspect | Details |
|--------|--------|
| **Input** | `image_url` (avatar photo), `audio_url` (e.g. cloned-voice narration), `resolution` (720p or 480p). |
| **Output** | MP4 with lip-sync, head/body motion; video includes audio. Same pattern as Replicate: use the video’s built-in audio for correct sync. |
| **API** | [fal.ai – veed/fabric-1.0](https://fal.ai/models/veed/fabric-1.0/api). Submit with `image_url` + `audio_url`, get `request_id`, poll `fal.queue.status` or use webhook, then `fal.queue.result` → `data.video.url`. Auth: `FAL_KEY`. |
| **Pricing (fal.ai)** | **Per second**: 480p ≈ $0.08/sec, 720p ≈ $0.15/sec. So ~\$2.40 (480p) or ~\$4.50 (720p) for a 30s clip; a 3‑minute story would be ~\$14.40 (480p) or ~\$27 (720p) — much higher than Replicate’s flat ~\$0.10/video. |
| **Length limit** | On fal.ai, **max 30 seconds per request**. Longer stories need multiple segments and stitching (not implemented in this app). |
| **Pros** | Image + audio → video; works with any avatar image and your cloned-voice audio; good lip-sync; 7× faster than many alternatives; available as a fal.ai endpoint. |
| **Cons** | Per-second cost and 30s cap make it expensive and awkward for long stories; Replicate SadTalker remains much cheaper for full-story avatar video. |

**Summary:** VEED Fabric 1.0 is a good technical fit for “avatar + cloned voice” (image + audio in → talking video out). For **short** clips (e.g. &lt; 30s) or premium/short-form product, it’s viable via fal.ai. For **full-story** avatar video, Replicate (or HeyGen) is more cost-effective unless you add segmenting and stitching for Fabric.

### VEED Lip Sync API (different use case)

**Lip Sync API** re-dubs **existing** video with new audio (lip remap). It does **not** create video from a single image + audio. For “avatar image + cloned-voice audio → new video,” use **Fabric 1.0**, not the Lip Sync API.

---

## Budget-friendly alternatives

If you need to reduce cost or move off Replicate:

| Option | Cost | Notes |
|--------|------|--------|
| **Replicate (current)** | ~\$0.10/video | Easiest; output includes audio, good sync when using video’s audio. |
| **VEED Fabric 1.0 (fal.ai)** | \$0.08/sec (480p), \$0.15/sec (720p); max 30s/request | Image + audio → video; good for avatar + cloned voice; expensive for long stories. |
| **Gooey.AI** | Free starter (500 credits ≈ 10 min); Creator \$20/mo (≈ 40 min); Business \$199/mo | [Lip sync API](https://docs.gooey.ai/guides/how-to-use-ai-lip-sync-generator/set-up-your-api-for-lipsync-with-local-folders); credits-based; good for trials and moderate usage. |
| **Fal.ai** | ~\$3/min (Sync Lipsync 2.0); Pro ~1.67× | [fal.ai/models/fal-ai/sync-lipsync](https://fal.ai/models/fal-ai/sync-lipsync); pay-per-use; **video** + audio → talking video (not single image). |
| **Sync Labs** | Free tier + from ~\$5/mo | [synclabs.so](https://docs.synclabs.so); **video + audio URL only** (no image + audio); credit-based. |
| **D-ID** | API pricing on request | [d-id.com/api](https://www.d-id.com/api); talking head from image + audio; 100+ languages; high throughput. |
| **Self-host SadTalker** | GPU only | Run [Faster-SadTalker-API](https://github.com/kenwaytis/faster-SadTalker-API) or [sadtalker-api](https://github.com/yungang/sadtalker-api) on a machine with NVIDIA GPU. No per-video fee; you pay for the instance (~\$0.20–0.60/hr on RunPod/Vast.ai for T4/RTX 4090). |
| **RunPod / Vast.ai / Lambda Labs** | ~\$0.20–0.60/hr (GPU) | Rent a GPU and run self-hosted SadTalker; good for burst or 24/7. |
| **LipSync Studio** | Free tier (limited sec/day); paid plans | [lipsync.studio](https://lipsync.studio); free daily credits at 360p/480p/720p; API from ~\$30/360 credits. |
| **A2E AI Avatar API** | From \$9.99/mo | [a2e.ai/api](https://www.a2e.ai/api); avatar, lip-sync, voice cloning; pay-as-you-go. |
| **InfiniteTalk (Kie.ai)** | Trial + paid | [kie.ai/infinitalk](https://kie.ai/infinitalk); image + audio → talking video; infinite-length option. |
| **Hugging Face Spaces** | Free (limited) | SadTalker Spaces exist but are often rate-limited or unstable; not ideal for production. |

## Flow verification (backend)

- **Config:** `AVATAR_VIDEO_ENABLED=true`, `AVATAR_VIDEO_PROVIDER=sadtalker`, `REPLICATE_API_TOKEN` set. Storage must be S3 (`STORAGE_TYPE=s3`) so `StoryAvatarVideoStoragePort` and `S3SignedUrlGenerator` are available; otherwise generation is marked FAILED and no PENDING records are left stuck.
- **Request:** `GET /api/v1/stories/{id}/stream-url?language=ta&voiceProfile=...` with authenticated parent. Controller resolves `parentId`, gets audio URL from `AudioStreamService`, then calls `avatarVideoService.getAvatarVideoUrl(id, source, parentId, language, voice)`.
- **AvatarVideoService:** If disabled, returns null. Else looks up `StoryAvatarVideo` by (storyId, source, parentId, language, voiceProfile). READY → return signed URL. PENDING/PROCESSING → return null. FAILED → return null. null → ensure parent has avatar image and story has audio, then create PENDING (unique constraint prevents duplicate on race), trigger async generation, return null.
- **Async:** Resolves audio URL and avatar image URL (signed). Calls Replicate `createPrediction`, polls until succeeded, downloads video with timeout, uploads to S3 via `StoryAvatarVideoStoragePort`, marks READY. On any failure (no storage, no audio, no avatar, Replicate error, timeout), marks FAILED.
- **Next request:** Same story + parent + language + voice returns `avatarVideoUrl` (signed) when status is READY.
