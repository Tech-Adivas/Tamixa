# Cloned Voice & Avatar – Frontend Integration

This doc describes how **cloned voice** and **avatar** (image + talking-head video) are wired from backend to mobile and web.

## Backend API

### Stream URL (unified)

- **Endpoint:** `GET /api/v1/stories/{id}/stream-url`
- **Query:** `language`, `voiceProfile` (optional), `storySource` (optional: `library` | `generated`)
- **Auth:** JWT; `parentId` is derived from the authenticated user (not sent by client).

**Response:** `StreamUrlResponse`

| Field           | Type     | Description                                                                 |
|----------------|-----------|-----------------------------------------------------------------------------|
| `streamUrl`    | string   | Signed URL for audio (library default/calm/cloned or generated story).     |
| `avatarUrl`    | string?  | Parent’s avatar **image** URL (for static display when no video).          |
| `avatarVideoUrl` | string? | Talking-head **video** URL when generation is READY (HeyGen/Replicate).    |
| `wordTimings`  | array?   | Word-level timings for transcript sync (optional; often null).              |

- For **cloned voice**, client sends `voiceProfile=cloned:{profileId}` (e.g. `cloned:1`). Backend may trigger on-demand TTS (longer latency; mobile uses a longer timeout).
- **Avatar video** is returned only when the job is READY and the parent is entitled (subscription). It is generated asynchronously; if not ready, `avatarVideoUrl` is null and client can show `avatarUrl` (static image).

Alternative path-based endpoints:

- `GET /api/v1/stories/library/{id}/stream-url` – library only
- `GET /api/v1/stories/generated/{id}/stream-url` – generated only

---

## Mobile (KMP Compose)

### Where it’s used

- **StoryApi.kt**
  - `getStreamUrl(storyId, language, voiceProfile?, storySource?)` calls `/api/v1/stories/{id}/stream-url` with `language`, `voiceProfile`, `storySource`.
  - For `voiceProfile?.startsWith("cloned:")`, request timeout is extended (e.g. 120s) for on-demand TTS.
  - Response is mapped to `StreamUrlResult.Url(url, avatarUrl, avatarVideoUrl, wordTimings)` (or UpgradeRequired / NotFound).

- **TamixaNavHost.kt** (story playback)
  - Fetches stream URL with `selectedVoice` (can be `cloned:1`, `calm`, `default`, etc.) and `storySource`.
  - On success: sets `streamUrl`, `streamAvatarUrl` (image), `streamAvatarVideoUrl` (talking-head).
  - If result is NotFound and a non-default voice was used, retries with default voice and uses that for playback (fallback).
  - **Playback:** If `streamAvatarVideoUrl` is present, uses `rememberAvatarVideoController` + `AvatarVideoSurface` (platform-specific: ExoPlayer on Android, AVPlayer on iOS). Otherwise plays `streamUrl` (or TTS fallback) and can show static `streamAvatarUrl` in the player UI.

- **AudioPlayerScreen.kt**
  - Receives `storytellingAvatarUrl` and `storytellingAvatarVideoContent`; shows avatar image when there’s no video, or the video surface when available.
  - Voice selector includes cloned options (`cloned:*`), labeled e.g. “My voice” via `Strings.voiceLabel`.

- **Voice / Avatar UX**
  - `MyVoiceAndAvatarScreen`, `VoiceUploadScreen`, `AvatarUploadScreen`: entry points for setting up voice and avatar.
  - Voice preference per story: `getVoicePreference` / `setVoicePreference` (e.g. “use cloned:1 for this story”).
  - Available voices: `getAvailableVoices(storyId, language)`; backend returns list including `cloned:{id}` when the parent has a profile.

### Summary

- **Cloned voice:** Sent as `voiceProfile=cloned:{id}` on stream-url; backend performs on-demand TTS when needed; mobile uses longer timeout and falls back to default voice if the request fails.
- **Avatar image:** From `avatarUrl` in stream-url response; used when no avatar video is available.
- **Avatar video:** From `avatarVideoUrl`; when non-null, mobile uses the platform video controller and full-screen video surface; no client-side polling for avatar-video status (user sees video on a later load when backend has marked it READY).

---

## Web (React)

- **api.ts**
  - `getStreamUrl(storyId, language, voiceProfile?, storySource?)` builds path by `storySource`: `library` → `/stories/library/{id}/stream-url`, `generated`/`mine` → `/stories/generated/{id}/stream-url`, else `/stories/{id}/stream-url`. Appends `language` and `voiceProfile` (when not default).
  - Returns `StreamUrlResponse` (streamUrl, avatarUrl, avatarVideoUrl).

- **Stories.tsx**
  - On play: calls `getStreamUrl` with selected voice (can be cloned) and story source; on failure with a specific voice, retries with default voice.
  - If `data.avatarVideoUrl` is set, sets that as the playing avatar video URL and uses it for playback; otherwise plays `data.streamUrl` as audio.

So on web, cloned voice and avatar (image + video) are integrated via the same stream-url response and the same fallback-to-default-voice behavior.

---

## Admin

- **Voice test / Voice & Avatar Studio** (e.g. `voice-test/page.tsx`): Uses admin stream-url and library endpoints with `voiceProfile=cloned:{id}` and `parentId` to test cloned voice and “Play with avatar” (narration + avatar video). Can delete narration for a story+language+voice to force regeneration.

---

## Alignment checklist

| Item | Backend | Mobile | Web |
|------|---------|--------|-----|
| Stream URL path | `/api/v1/stories/{id}/stream-url` (and library/generated variants) | Same | Same (path by storySource) |
| voiceProfile for cloned | `cloned:{profileId}` | Sent as `voiceProfile` | Sent as `voiceProfile` |
| parentId | From JWT | Not sent (auth) | Not sent (auth) |
| streamUrl | ✅ | ✅ Used for audio | ✅ Used for audio |
| avatarUrl | ✅ Optional | ✅ Static avatar when no video | ✅ Available in response |
| avatarVideoUrl | ✅ When READY + entitled | ✅ Drives video surface | ✅ Used for video playback |
| Fallback to default voice on failure | N/A | ✅ Retry with null voice | ✅ Retry with null voice |
| Long timeout for cloned | N/A | ✅ 120s | — |

---

## Possible improvements

1. **Avatar video status polling (mobile):** Backend exposes `GET /stories/{id}/avatar-video-status`. Mobile could poll when `avatarVideoUrl` is null but the user has premium, to show “generating…” and then switch to video when READY.
2. **wordTimings:** Backend DTO supports `wordTimings`; controllers currently pass only (url, avatarUrl, avatarVideoUrl). If word timings are added later, mobile already has the field in `StreamUrlResult.Url` and can use it for highlight sync.
