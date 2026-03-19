# Avatar Video Troubleshooting

Operational guide for avatar video generation failures and recovery.

---

## Overview

Avatar video is the talking-head video generated from story audio + parent's avatar image. It is **lazy-generated**: first stream request triggers generation. Providers: HeyGen (primary) → Replicate (SadTalker) → D-ID → Gooey.

---

## Failure Modes

| Symptom | Cause | Recovery |
|---------|-------|----------|
| **VIDEO_FAILED** | HeyGen/Replicate/D-ID error, timeout, or invalid input | Next stream request auto-retries (deletes FAILED, recreates PENDING). Or delete via admin and retry. |
| **No audio for avatar** | Audio pipeline not run or failed for that language | Run pipeline in Story to Speech; ensure audio exists before avatar. |
| **No avatar image** | Parent has not uploaded avatar | Parent must upload avatar in app; admin can use Voice & Avatar Studio with test parent. |
| **Stuck PENDING/PROCESSING** | Provider slow or hung | Wait (HeyGen can take 2–5 min). If > 15 min, delete avatar and retrigger. |
| **Provider not configured** | `HEYGEN_*` / `REPLICATE_*` env vars missing | Configure at least one provider; check `avatarVideo.enabled=true`. |

---

## Recovery Steps

### 1. Auto-retry (no action)

When status is **FAILED**, the next stream request for that (story, parent, language, voice) automatically:
1. Deletes the FAILED record
2. Creates new PENDING
3. Triggers generation again

Parent or admin just needs to request the stream URL again (e.g. play in app or Voice & Avatar Studio).

### 2. Manual delete + retry (admin)

1. Go to **Voice & Avatar Studio** (or use API).
2. Select story, parent, language, voice.
3. Click **Regenerate avatar** (or call `DELETE /api/v1/admin/stories/{id}/avatar-video?parentId=&language=&voiceProfile=`).
4. Next Play/stream request will regenerate.

### 3. Check provider and config

- **HeyGen**: `HEYGEN_API_KEY`, `HEYGEN_AVATAR_ID` (or template)
- **Replicate**: `REPLICATE_API_TOKEN` (SadTalker fallback)
- **Avatar enabled**: `app.avatar-video.enabled=true` (default)

```bash
# Verify avatar provider
curl -H "Authorization: Bearer $TOKEN" .../api/v1/admin/health
# Response may include avatar provider info
```

### 4. Common errors

| Error / Log | Meaning |
|-------------|---------|
| `No public audio URL` | Audio not generated for this story+language; run pipeline first. |
| `Parent has no avatar` | Parent has not uploaded avatar image. |
| `HeyGenCreateVideoException` | HeyGen API error; check quota, avatar ID, input format. |
| `Replicate prediction failed` | SadTalker failed; check Replicate logs or try HeyGen. |

---

## Prevention

1. **Audio before avatar** — Ensure pipeline has completed for the story + language before testing avatar.
2. **Avatar upload** — Parent must complete avatar upload before avatar video can be generated.
3. **Rate limits** — HeyGen/Replicate have limits; avoid bulk avatar generation in short bursts.

---

## Related

- [STORY_AUDIO_AVATAR_FLOW.md](../admin/STORY_AUDIO_AVATAR_FLOW.md) — End-to-end flow
- [NARRATION_ARCHITECTURE.md](../backend/NARRATION_ARCHITECTURE.md) — Audio pipeline
- Voice & Avatar Studio (admin) — Test avatar with selected parent/story/language
