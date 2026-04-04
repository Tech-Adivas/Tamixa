# Environment variables reference

Single reference for backend and admin environment variables. Copy from root `.env.example` and admin `.env.example` (or `admin/.env.local`) and override as needed. **Never commit `.env`**; use a secrets manager in production.

**See also:** [Tamixa cost & marketing plan](TAMIXA_COST_AND_MARKETING_PLAN.md) — how variable COGS (LLM, TTS, SMS, storage, optional avatar/voice providers) maps to this surface; [Deployment plan](DEPLOYMENT_PLAN.md) for prod rollout. **Pricing guardrails:** story/voice quotas and trial/grace defaults live in `AppProperties.SubscriptionProperties` (e.g. `SUBSCRIPTION_FREE_STORIES_PER_MONTH`, `SUBSCRIPTION_STARTER_STORIES_PER_MONTH`) and plan semantics in `SubscriptionPlan` — see the “Plan-level guardrails” section in the cost & marketing doc.

## Feature flags & when they are required

| Variable | Effect | Required when |
|----------|--------|----------------|
| `SEED_ADMIN_ENABLED` | Enables POST `/api/v1/dev/seed-admin` to create/reset admin user. | **Never in production.** Use only in `dev` profile; ensure prod does not have `dev` profile or this set to true. |
| `AVATAR_VIDEO_ENABLED` | Enables talking-head avatar video. | Set to `true` only when avatar video is used; requires provider-specific keys. |
| `VOICE_CLONING_ENABLED` | Enables voice cloning from uploaded samples. | Set to `true` when offering cloned voices; requires provider key (e.g. `GOOGLE_CLOUD_TTS_API_KEY`, `ELEVENLABS_API_KEY`). |
| `SHARE_CLIP_ENABLED` | Enables shareable clip generation (Premium+). | Set to `true` when share clip feature is on; may require `SHARE_CLIP_FFMPEG_ENABLED` and ffmpeg. |
| `app.rate-limit.enabled` | Backend general rate limiting. | Default true; set via `application.yml` or override. |
| `NOTIFICATIONS_EMAIL_REMOTE_ENABLED` | Delegates magic-link email to a **notifications HTTP service** over internal HTTP (any deployment that implements the contract). | When `true`, set `NOTIFICATIONS_EMAIL_BASE_URL` and `NOTIFICATIONS_INTERNAL_API_KEY`; run that service separately and configure `SENDGRID_API_KEY` (or equivalent) there. See [MICROSERVICES_PLATFORM.md](MICROSERVICES_PLATFORM.md). |

## Backend (root `.env`)

- **API / URLs**: `API_BASE_URL`, `AUDIO_PUBLIC_BASE_URL`, `MAGIC_LINK_BASE_URL`, `TAMIXA_WEB_APP_URL` (passwordless email links)
- **Auth / Email**: `SENDGRID_API_KEY` (in-process SendGrid when remote notifications off), `NOTIFICATIONS_EMAIL_REMOTE_ENABLED`, `NOTIFICATIONS_EMAIL_BASE_URL`, `NOTIFICATIONS_INTERNAL_API_KEY`, `DEV_OTP_CODE`
- **Database / Redis**: Set via `SPRING_DATASOURCE_*`, `REDIS_HOST`, `REDIS_PORT` (or defaults in `application.yml`)
- **Storage**: `STORAGE_TYPE`, `S3_BUCKET`, `S3_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`
- **Translation**: `TRANSLATION_PROVIDER`
- **Primary LLM**: `AI_LLM_PROVIDER` (`openai` \| `gemini`). When `gemini`: `GEMINI_API_KEY`, `GEMINI_MODEL`, `GEMINI_BASE_URL`, `GEMINI_READ_TIMEOUT_MS`, `GEMINI_MODERATION_REQUIRED` (see `application-prod.yml`).
- **OpenAI**: `OPENAI_API_KEY` (required when `AI_LLM_PROVIDER=openai` and for OpenAI translation/TTS paths), `OPENAI_READ_TIMEOUT_MS`, `OPENAI_BASE_URL`
- **Cover still image**: `AI_COVER_IMAGE_PROVIDER` (`openai` = DALL·E 3 + `OPENAI_API_KEY`; `gemini` = Gemini image + `GEMINI_API_KEY`). Optional: `AI_COVER_IMAGE_GEMINI_MODEL`, `AI_COVER_IMAGE_GEMINI_ASPECT_RATIO`, `AI_COVER_IMAGE_GEMINI_IMAGE_SIZE`, `AI_COVER_IMAGE_GEMINI_USE_IMAGE_CONFIG`.
- **Cover GIF (Veo)**: `COVER_ANIMATION_VEO_ENABLED`, `GEMINI_API_KEY`, `COVER_ANIMATION_VEO_*` (see root `.env.example`)
- **Narration**: `NARRATION_TTS_PROVIDER`, `NARRATION_TAMIXA_TTS_DEFAULT_FILE`, `GOOGLE_CLOUD_TTS_API_KEY`, and provider-specific options (see `.env.example`)
- **Avatar video**: `AVATAR_VIDEO_ENABLED`, `AVATAR_VIDEO_PROVIDER`, `REPLICATE_API_TOKEN`, `HEYGEN_API_KEY`, etc.
- **Voice cloning**: `VOICE_CLONING_ENABLED`, `VOICE_CLONING_PROVIDER` (default `elevenlabs`; or `google`). For **ElevenLabs** (default): set `ELEVENLABS_API_KEY`; upload reference only, then run job (admin: upload at `/parents/{parentId}/voice/upload`, then POST `/parents/{parentId}/voice/{voiceProfileId}/run-job`). For **Google**: set `GOOGLE_CLOUD_TTS_API_KEY`; upload reference and consent, then POST `.../consent` with file.
- **Share clip**: `SHARE_CLIP_ENABLED`, `SHARE_CLIP_CLIPS_PER_MONTH`, `SHARE_BASE_URL`, `SHARE_CLIP_FFMPEG_ENABLED`
- **Pipeline**: `TRANSLATION_PIPELINE_PARALLELISM`, `PIPELINE_ON_SUBMIT_ONLY`, `AUDIO_AFTER_APPROVAL`, `NARRATION_MAX_CONCURRENT_REWRITE`

See root [.env.example](../.env.example) for full list and comments.

## Admin (Next.js)

- **API URL**: `NEXT_PUBLIC_API_URL` — backend base URL (e.g. `http://localhost:8080`). Required when admin runs separately from backend.
- **Mock API**: `NEXT_PUBLIC_USE_MOCK_API` — set to `true` to use mock API for development (optional).

See [admin/.env.example](../admin/.env.example) for admin-specific vars.

## Production checklist

- [ ] `dev` profile is **not** active (no `SEED_ADMIN_ENABLED`, no dev seed endpoints).
- [ ] All secrets (API keys, DB password, JWT secret) come from environment or secrets manager, not from repo.
- [ ] CORS and rate limits are configured for production (see `application.yml` and [SECURITY.md](SECURITY.md)).
