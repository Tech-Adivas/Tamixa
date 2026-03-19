# Narration Engine Architecture

## Overview

The Tamixa Narration Engine produces conversational audio for translated stories using neural TTS, with emotional realism, premium voice monetization, and scale-ready infrastructure.

---

## 1. Emotional Realism: Deterministic-First

**Why deterministic rules first?**

- **Safety**: Kids' content must never hallucinate. AI-generated emotion labels could introduce inappropriate or nonsensical tags.
- **Predictability**: Rule-based tagging (quoted text → DIALOGUE, "suddenly" → SOFT_SUSPENSE) produces consistent, auditable output.
- **Cost**: No extra LLM calls for emotion tagging; keeps per-story cost low at scale.
- **Optional AI assist**: The design allows future AI enhancement for edge cases, but the fallback remains deterministic. If AI deviates too much (word count validation), we fall back to plain SSML.

---

## 2. Premium Voice Gating: Server-Enforced

**Why server-side validation?**

- **Security**: Clients cannot bypass monetization. A modified app or API client cannot access premium voices without entitlement.
- **Single source of truth**: Subscription state lives server-side; `subscription.voicePremium` is authoritative.
- **Auditability**: All premium voice access is logged via `premium_voice_usage` metric and stream access logs.
- **Flow**: `SubscriptionGuard.enforcePremiumVoiceAccess()` throws `UpgradeRequiredException` (402) before returning any stream URL. The stream URL is never exposed to non-entitled users.

---

## 3. Concurrency Limiter: Why It Matters

**Why cap concurrent TTS jobs?**

- **Provider rate limits**: Neural TTS (Azure, Google, OpenAI) enforce per-minute or per-second quotas. Exceeding them causes **429 TOO_MANY_REQUESTS** and failed narrations.
- **Resource exhaustion**: Unbounded concurrent jobs would exhaust executor threads, memory, and connection pools.
- **100k scale**: At high throughput, a burst of requests must be queued, not rejected. The semaphore + bounded executor ensure steady throughput without overloading downstream.
- **Metrics**: `tts_concurrency_current` gauge allows alerting when we approach the limit.

**When you see 429 in logs:** The shared `RestTemplate` logs a WARN with the upstream URI and `Retry-After` (if present). Reduce load by lowering `app.narration.max-concurrent-tts` and/or `app.narration.max-concurrent-rewrite` (e.g. in `.env`: `NARRATION_MAX_CONCURRENT_TTS=3`, `NARRATION_MAX_CONCURRENT_REWRITE=3`). **Note:** `max-concurrent-rewrite` must be >= `translation-pipeline.parallelism` or tasks will timeout waiting for a rewrite permit. OpenAI and TTS clients that use `@Retryable` will retry with backoff.

---

## 4. Voice Cloning (ElevenLabs)

**Parent uploads audio sample → ElevenLabs creates cloned voice → stories narrated in parent's voice.**

- **Upload**: `POST /voice/upload` stores `elevenlabs_voice_id` in `voice_profiles`.
- **On-demand**: When user requests stream with `voiceProfile=cloned:{id}` and no audio exists, `AudioStreamService` triggers `StoryProcessingOrchestrator` to generate narration synchronously.
- **Ownership**: Only the parent who owns the voice profile can request narration with that clone.
- **Configuration**: `VOICE_CLONING_ENABLED=true`, `ELEVENLABS_API_KEY` required.
- **Routing**: Default/calm narration uses **Google Cloud TTS** (default) via `NeuralVoiceStrategy`. Cloned voice: `ClonedVoiceStrategy` uses **HeyGen TTS** (when profile has `heygen_voice_id`), then **ElevenLabs**, then **XTTS**.

---

## Idempotency

- **Per (translationId, voiceProfile)**: `StoryNarrationAudio` uniqueness ensures we never regenerate the same audio twice.
- **existsByTranslationIdAndVoiceProfileAndStatus(READY)**: Skips TTS if audio already exists.
- **Script**: One script per translation; shared across all voice profiles. Regeneration of one voice does not invalidate others.

---

## Exception Hierarchy

- `EmotionValidationException`: Emotion tagging word count deviation exceeds tolerance → fallback to plain SSML.
- `UpgradeRequiredException`: Premium voice requested without entitlement → 402.
- `NarrationJobCapacityExceededException`: Concurrency limit reached → 503.
- `SafetyValidationException`: Content safety check failed → reject.
- No blocking calls in request thread: TTS runs on bounded executor; stream URL generation is synchronous but fast (DB + optional signing).
