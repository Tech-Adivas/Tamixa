# Avatar Lip-Sync: Partner Comparison & Implementation Plan

**Goal:** Implement avatar lip-sync for story narration by choosing the best partner(s) for **cost**, **quality**, and **performance**. This plan compares ElevenLabs-recommended and existing partners and outlines how to add new providers to the current pipeline.

---

## Current State

- **Backend:** [AvatarVideoService](backend/src/main/kotlin/com/tamixa/application/avatar/AvatarVideoService.kt) orchestrates avatar video generation. It supports two providers today (selected via `app.avatar-video.provider`):
  - **HeyGen** – talking photo + audio → video (premium quality, higher cost).
  - **Replicate SadTalker** – image + audio URL → Replicate prediction → poll → video.
- **Flow:** Parent has avatar image; story has narration audio (e.g. ElevenLabs cloned voice). On first stream-url request, backend creates a PENDING `StoryAvatarVideo`, triggers async generation, returns `avatarVideoUrl` when READY (or null until then). Mobile plays video when `avatarVideoUrl` is present.
- **Config:** [AppProperties.AvatarVideoProperties](backend/src/main/kotlin/com/tamixa/infrastructure/config/AppProperties.kt) – `provider`, `heygenApiKey`, `replicateApiToken`, poll settings.

---

## Partner Comparison (Cost, Quality, Performance)

Assumption: **typical story length 3–5 minutes** (180–300 seconds of video). All partners take **audio URL + image (or video) URL** and return a lip-synced video (async or sync).

| Partner | Cost (per 5‑min video) | Quality | Performance | ElevenLabs integration | Notes |
|--------|--------------------------|--------|-------------|------------------------|-------|
| **Replicate SadTalker** | **~$0.10 per run** (flat) | Good: talking-head, natural head motion, 3DMM | ~75 s per run | No (you send audio URL from your CDN) | **Lowest cost.** Already integrated. Best for volume. |
| **Gooey.AI** | ~$0.50–1.20 per 5 min (credit-based; 5‑sec increments) | Good; less public comparison data | Async, similar to others | Can use your audio (e.g. from ElevenLabs) | Mid cost. Creator $20/mo ≈ 40 lip-sync min. |
| **Sync Labs** (sync.so) | **$6–15 per 5 min** (per second: lipsync-1.9 $0.02–0.025/s, lipsync-2 $0.04–0.05/s) | **Best:** Lipsync-2 Pro, style preservation, multi-language | Async, poll or webhook | **Native:** “Own TTS API key” + ElevenLabs; can pass text + voice | **Best quality.** Creator: 5 min max, 3 concurrent. |
| **HeyGen** | **$5–30 per 5 min** (Avatar III $0.0167/s, Avatar IV $0.1/s) + $1 photo upload | High; commercial talking-photo | Async | No direct ElevenLabs tie-in | Already integrated. Premium tier. |

### Cost summary (per 5‑minute story)

- **Replicate SadTalker:** ~**$0.10** (flat per run) → **best cost**.
- **Gooey.AI:** ~**$0.50–1.20** (credit-based).
- **Sync Labs lipsync-1.9:** ~**$6–7.50** (e.g. $0.02–0.025/s).
- **Sync Labs lipsync-2:** ~**$12–15** ($0.04–0.05/s).
- **HeyGen:** ~**$5–30** depending on engine.

### Quality & performance

- **Quality:** Sync Labs (Lipsync-2 / Lipsync-2 Pro) and HeyGen are top tier. SadTalker is good for talking-head with head motion. Gooey is good but less documented in public comparisons.
- **Performance:** All are async (create job → poll or webhook → download/store). Replicate typically ~75 s; others in the same order of magnitude.
- **ElevenLabs:** Sync Labs supports “Own TTS API key” and native ElevenLabs integration (text + voice → lip-sync in one flow). Others use “audio URL + image URL” which fits your current pipeline (ElevenLabs TTS → your CDN → partner).

---

## Recommendation: Best Partner by Priority

1. **Cost-first (default):** **Replicate SadTalker** – already integrated; ~$0.10 per video. Keep as default for high volume and cost-sensitive rollout.
2. **Quality-first (premium):** **Sync Labs** – best lip-sync quality, native ElevenLabs option, 5 min max on Creator ($19/mo + usage). Add as optional provider (e.g. `provider=synclabs`) for premium or A/B quality.
3. **Mid-tier / alternative:** **Gooey.AI** – moderate cost, reasonable quality. Add as optional provider for flexibility or if Sync/Replicate have capacity or regional issues.
4. **Already integrated:** **HeyGen** – keep for customers who need maximum polish and accept higher cost.

**Suggested default:** `app.avatar-video.provider=sadtalker` (Replicate) for cost; expose Sync Labs (and optionally Gooey) via config or feature flag for premium/quality paths.

---

## Implementation Plan

### Phase 1: Unify behind a port (optional but recommended)

- Introduce a **TalkingHeadRendererPort** (or keep the current “provider” selection) so all lip-sync providers implement the same contract:
  - **Input:** narration audio URL (signed), avatar image URL (signed), optional metadata (storyId, language).
  - **Output:** video URL or bytes (or job ID + poll until done), then backend uploads to S3 and saves `StoryAvatarVideo`.
- Current code already does this per provider inside `AvatarVideoService` (HeyGen vs Replicate). Refactoring to a port + adapters would make adding Sync Labs and Gooey a single new adapter each.

### Phase 2: Add Sync Labs as a provider

- **Config:** Add to `AvatarVideoProperties`:
  - `synclabsApiKey: String`
  - When `provider=synclabs`, use Sync Labs.
- **Client:** New `SyncLabsLipSyncClient` (or adapter implementing the port):
  - Call `POST https://api.sync.so/v2/generate` with:
    - `inputs`: video (avatar image or a single-frame “video” URL) + audio URL.
    - Model: `lipsync-2` (or `lipsync-2-pro` on Scale plan).
    - Optional: `webhookUrl` for completion callback; else poll `GET /v2/generate` for status.
  - On success: download video from returned URL, upload to existing `StoryAvatarVideoStoragePort`, save `StoryAvatarVideo` with READY status.
- **Cost awareness:** Sync charges per second; log duration and consider alerting if cost per story exceeds a threshold (e.g. 5 min = 300 s × $0.05 ≈ $15).

References:
- [Sync API: Create Generation](https://docs.sync.so/api-reference/endpoint/generate/post-generate)
- [Sync Billing](https://docs.sync.so/billing)
- [Sync Pricing](https://lp.sync.so/pricing) (lipsync-2, lipsync-2-pro per second)

### Phase 3: Add Gooey.AI as a provider (optional)

- **Config:** `gooeyApiKey` (or base URL + key). When `provider=gooey`, use Gooey.
- **Client:** New `GooeyLipSyncClient`:
  - Use Gooey Lip Sync API (audio URL + image/video URL → job or direct video).
  - Follow their auth (e.g. bearer token), poll or webhook for completion, then download and store via `StoryAvatarVideoStoragePort`.
- **Cost:** Credit-based; log credits consumed per request if the API exposes it.

References:
- [Gooey.AI Lip Sync](https://docs.gooey.ai/guides/how-to-use-ai-lip-sync-generator)
- [Gooey Pricing](https://www.help.gooey.ai/pricing) (credits, 5‑sec increments)

### Phase 4: Config and feature behavior

- **application.yml / .env.example:** Document all four providers:
  - `app.avatar-video.provider`: `sadtalker` | `heygen` | `synclabs` | `gooey`
  - Provider-specific keys: `replicateApiToken`, `heygenApiKey`, `synclabsApiKey`, `gooeyApiKey`.
- **AvatarVideoService:** Resolve provider from config; call the corresponding client (or port implementation). Keep existing behavior: on first request create PENDING, trigger async generation, return null until READY; then return signed `avatarVideoUrl`.
- **Mobile:** No change; already uses `avatarVideoUrl` from stream-url response.

### Phase 5: Cost and quality observability

- **Logging:** Log provider used, story id, duration (when available), and whether generation succeeded or failed.
- **Optional metrics:** Counter per provider (success/failure), histogram of generation time, and if possible cost proxy (e.g. duration × known rate for Sync).
- **Docs:** Update [AVATAR_VIDEO.md](AVATAR_VIDEO.md) with partner comparison, config for each provider, and cost guidance (e.g. “Replicate for cost, Sync for quality”).

---

## Summary Table: When to Use Which Partner

| Priority | Partner | Config value | Typical use |
|----------|---------|--------------|-------------|
| **Cost** | Replicate SadTalker | `sadtalker` | Default; high volume; ~$0.10/video |
| **Quality** | Sync Labs | `synclabs` | Premium; best lip-sync; native ElevenLabs; ~$6–15/5 min |
| **Mid** | Gooey.AI | `gooey` | Alternative; ~$0.50–1.20/5 min |
| **Premium** | HeyGen | `heygen` | Already integrated; highest polish; ~$5–30/5 min |

---

## References

- [Sync Labs API – Create Generation](https://docs.sync.so/api-reference/endpoint/generate/post-generate)
- [Sync Labs Billing](https://docs.sync.so/billing)
- [Sync Labs Pricing](https://lp.sync.so/pricing)
- [Gooey.AI Lip Sync](https://docs.gooey.ai/guides/how-to-use-ai-lip-sync-generator)
- [Gooey.AI Pricing](https://www.help.gooey.ai/pricing)
- [Replicate SadTalker](https://replicate.com/cjwbw/sadtalker) (~$0.10/run)
- [HeyGen API](https://docs.heygen.com/reference/limits) (talking photo + video)
- Existing: [AvatarVideoService](backend/src/main/kotlin/com/tamixa/application/avatar/AvatarVideoService.kt), [AVATAR_VIDEO.md](AVATAR_VIDEO.md)
