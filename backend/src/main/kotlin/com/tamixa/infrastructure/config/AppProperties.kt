package com.tamixa.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Central application configuration. Prefer this over @Value for type safety and grouping.
 * All values are overridden via application.yml and environment variables (e.g. JWT_SECRET).
 */
@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val auth: AuthProperties = AuthProperties(),
    val storage: StorageProperties = StorageProperties(),
    val cors: CorsProperties = CorsProperties(),
    val rateLimit: RateLimitProperties = RateLimitProperties(),
    val jwt: JwtProperties = JwtProperties(),
    val openai: OpenAIProperties = OpenAIProperties(),
    /**
     * Primary LLM provider for story generation, moderation (when using this client), pipeline rewrite, etc.
     * See [com.tamixa.infrastructure.openai.OpenAIClient] vs [com.tamixa.infrastructure.gemini.GeminiLlmClient].
     */
    val llm: LlmProperties = LlmProperties(),
    /**
     * Still cover illustration (before optional Veo GIF): OpenAI DALL·E 3 or Gemini native image.
     * Uses [OpenAIProperties.apiKey] when [ImageGenerationProperties.provider] is openai;
     * [LlmProperties.GeminiLlmProperties.apiKey] when provider is gemini.
     */
    val imageGeneration: ImageGenerationProperties = ImageGenerationProperties(),
    /** Cover GIF pipeline: FFmpeg MP4→GIF; optional Google Veo Lite image-to-video. */
    val coverAnimation: CoverAnimationProperties = CoverAnimationProperties(),
    val story: StoryProperties = StoryProperties(),
    val audio: AudioProperties = AudioProperties(),
    val cdn: CdnStreamProperties = CdnStreamProperties(),
    val voice: VoiceProperties = VoiceProperties(),
    val subscription: SubscriptionProperties = SubscriptionProperties(),
    val aiTokenLimits: AiTokenLimitProperties = AiTokenLimitProperties(),
    val translationPipeline: TranslationPipelineProperties = TranslationPipelineProperties(),
    val narration: NarrationProperties = NarrationProperties(),
    val export: ExportProperties = ExportProperties(),
    val avatarVideo: AvatarVideoProperties = AvatarVideoProperties(),
    val voiceCloning: VoiceCloningProperties = VoiceCloningProperties(),
    val shareClip: ShareClipProperties = ShareClipProperties(),
    val bulkJob: BulkJobProperties = BulkJobProperties(),
    val push: PushProperties = PushProperties(),
    /** Soft-deleted library stories: retention before permanent DB delete. */
    val libraryStorySoftDelete: LibraryStorySoftDeleteProperties = LibraryStorySoftDeleteProperties(),
    /**
     * Local E2E helpers for Digital Survival Guide seeds (`story_owner = seed:digital-survival-guide-v1`).
     * All flags default false; enable only in dev profile. See DigitalSurvivalDevController.
     */
    val digitalSurvivalDev: DigitalSurvivalDevProperties = DigitalSurvivalDevProperties(),
    /** Optional reusable Python guardrails HTTP service (separate repo / container). */
    val guardrailsService: GuardrailsServiceProperties = GuardrailsServiceProperties(),
    /** AI control plane (governance registry + workflow runs). */
    val controlPlane: ControlPlaneProperties = ControlPlaneProperties(),
    /**
     * Strangler: send passwordless magic-link email via extracted notifications service (HTTP)
     * instead of in-process SendGrid. When [NotificationsEmailProperties.remoteEnabled] is true,
     * [com.tamixa.infrastructure.notification.NotificationEmailDispatcher] prefers the remote client.
     */
    val notificationsEmail: NotificationsEmailProperties = NotificationsEmailProperties(),
) {

    data class NotificationsEmailProperties(
        val remoteEnabled: Boolean = false,
        /** Base URL of notifications service (no path), e.g. http://notifications:8100 */
        val baseUrl: String = "",
        /** Shared secret; must match notifications service NOTIFICATIONS_INTERNAL_API_KEY. */
        val internalApiKey: String = "",
    )

    data class ControlPlaneProperties(
        /**
         * When true, each parent story generation registers a workflow run (`execute` → `markRunCompleted` / `markRunFailed`)
         * against [storyProjectCode] / [storyWorkflowKey]. Requires Flyway V75+V76 (seed project + workflow).
         */
        val storyWorkflowIntegrationEnabled: Boolean = false,
        /** Control plane project code for story runs (seed default: `tamixa`). */
        val storyProjectCode: String = "tamixa",
        /** Workflow key for story runs (seed default: `story.create_with_narration`). */
        val storyWorkflowKey: String = "story.create_with_narration",
    )

    data class DigitalSurvivalDevProperties(
        /**
         * When true (dev): parent library API responses rewrite interactive graph segment `audioUrl` values
         * from `https://cdn.tamixa.app/library/sim/digital-survival-guide/...` to the local dev placeholder MP3 path.
         */
        val rewriteInteractiveGraphAudioToDevPlaceholder: Boolean = false,
        /**
         * When true (dev): `POST .../dev/digital-survival/prepare-e2e-seed` may run (still requires `dev` profile).
         */
        val allowPrepareE2eSeedEndpoint: Boolean = false,
    )

    data class LibraryStorySoftDeleteProperties(
        /** Days to keep soft-deleted `library_stories` rows before purge job runs DELETE. */
        val retentionDays: Long = 30,
        val purgeEnabled: Boolean = true,
        /** Spring @Scheduled six-field cron (sec min hour day month weekday). */
        val purgeCron: String = "0 0 4 * * *"
    )

    data class GuardrailsServiceProperties(
        val enabled: Boolean = false,
        val baseUrl: String = "",
        val apiKey: String = "",
        val connectTimeoutMs: Long = 2000,
        val readTimeoutMs: Long = 15000,
        /** When true, unreachable/5xx from guardrails service skips remote check (Kotlin pipeline still runs). */
        val failOpenOnError: Boolean = false
    )

    data class PushProperties(
        val fcmServerKey: String = "",
        val apnsKeyId: String = "",
        val apnsTeamId: String = "",
        val apnsKeyPath: String = "",
        val apnsBundleId: String = "com.tamixa.app",
        val apnsProduction: Boolean = false
    )

    data class BulkJobProperties(
        val useRedis: Boolean = false
    )

    data class ShareClipProperties(
        val enabled: Boolean = true,
        val clipsPerMonthPremiumPlus: Int = 10,
        val shareBaseUrl: String = "https://tamixa.com",
        val ffmpegEnabled: Boolean = false
    )

    data class AvatarVideoProperties(
        val enabled: Boolean = false,
        val provider: String = "sadtalker",
        val heygenApiKey: String = "",
        val replicateApiToken: String = "",
        val replicateModelVersion: String = "cjwbw/sadtalker:a519cc0cfebaaeade068b23899165a11ec76aaa1d2b313d40d214f204ec957a3",
        val gooeyApiKey: String = "",
        val gooeyRecipeId: String = "",
        val gooeyTtsProvider: String = "ELEVEN_LABS",
        val gooeyBaseUrl: String = "https://api.gooey.ai",
        val didApiKey: String = "",
        val didBaseUrl: String = "https://api.d-id.com",
        val pollIntervalSeconds: Int = 20,
        val maxPollAttempts: Int = 30,
        val connectTimeoutMs: Long = 30000,
        val readTimeoutMs: Long = 120000
    )

    data class VoiceCloningProperties(
        val enabled: Boolean = false,
        /** Primary provider: elevenlabs | fishaudio | google | xtts. Default from application.yml: google */
        val provider: String = "google",
        /**
         * When true and provider=google, failed Google clone/synthesize attempts may fall back to ElevenLabs
         * if ELEVENLABS_API_KEY is configured.
         */
        val allowElevenLabsFallback: Boolean = true,
        /**
         * When true and provider=google, no-consent reference-only jobs and failed Google clones may use Fish Audio
         * if FISH_AUDIO_API_KEY is configured (useful when ElevenLabs is unavailable).
         */
        val allowFishAudioFallback: Boolean = false,
        val elevenLabsApiKey: String = "",
        val elevenLabsBaseUrl: String = "https://api.elevenlabs.io",
        val fishAudioApiKey: String = "",
        val fishAudioBaseUrl: String = "https://api.fish.audio",
        /** Fish TTS model header (e.g. s2-pro, s1). */
        val fishAudioTtsModel: String = "s2-pro",
        /** Google Cloud API key for Chirp 3 Instant Custom Voice (when provider=google). */
        val googleCloudApiKey: String = "",
        /** Base URL for self-hosted XTTS-style voice cloning service (optional). */
        val xttsBaseUrl: String = "",
        /** Timeout for XTTS synthesize calls in seconds (synthesis + model load can take 2–3 min). */
        val xttsTimeoutSeconds: Int = 300
    )

    data class ExportProperties(
        /** Days until export file link expires (job expires_at). */
        val expiryDays: Int = 7,
        /** Signed URL validity in minutes when listing completed exports. */
        val signedUrlExpiryMinutes: Long = 60,
        /** Cron for processing pending export jobs. Default: every 2 minutes. */
        val processCron: String = "0 " + "*" + "/2 * * * *",
        /** DB page size when streaming stories into export JSON (memory vs round-trips). */
        val storyPageSize: Int = 200,
        /** DB page size for consent rows in export. */
        val consentPageSize: Int = 100
    )

    /** Auth: dev OTP bypass, web passwordless bypass, web base URL for magic links, etc. */
    data class AuthProperties(
        /** When set (e.g. "123456"), accept this code with any phone for dev/testing. Also returned in send response. */
        val devOtpCode: String? = null,
        /** When set (e.g. "123456"), accept this code for web app passwordless login with any email (dev only). */
        val devPasswordlessCode: String? = null,
        /** Web app base URL for passwordless magic links (e.g. https://app.tamixa.com or http://localhost:3000). */
        val webBaseUrl: String = "http://localhost:3000"
    )

    data class NarrationProperties(
        val dailyTokenLimit: Int = 100000,
        val maxNarrationTokens: Int = 1024,
        /** TTS synthesize timeout; non-Tamil (longer content) needs more time. */
        val ttsTimeoutSeconds: Int = 60,
        val ttsMaxRetries: Int = 2,
        val voiceProfile: String = "default",
        val blocklistKeywords: String = "",
        val simulatedTtsDelayMs: Long = 0,
        /** Google Cloud TTS API key (shared with voice cloning when provider=google). */
        val googleTtsApiKey: String = "",
        /** Read timeout (seconds) per HTTP chunk for Google TTS. Full story = many chunks; 120s allows slow Chirp3/WaveNet. */
        val googleTtsReadTimeoutSeconds: Int = 120,
        /** When true, use per-segment TTS (slower but more control). Default false. */
        val perSegmentTts: Boolean = false,
        val asyncExecutorCoreSize: Int = 8,
        val asyncExecutorMaxSize: Int = 32,
        val asyncQueueCapacity: Int = 200,
        val maxConcurrentJobs: Int = 100,
        /** Max concurrent TTS API calls (separate from job limiter; protects provider rate limits). */
        val maxConcurrentTts: Int = 8
    )

    data class CorsProperties(
        /** Comma-separated exact origins. Empty or star means unset unless allowed-origin-patterns is set (see application.yml). */
        val allowedOrigins: String = "*",
        /** Comma-separated CORS origin patterns (Spring allowedOriginPatterns). Env: CORS_ALLOWED_ORIGIN_PATTERNS. */
        val allowedOriginPatterns: String = ""
    )

    data class RateLimitProperties(
        val enabled: Boolean = true,
        /** When true, use Redis for rate limiting (multi-instance). Requires Redis. Default false. */
        val useRedis: Boolean = false,
        /**
         * When false and Redis errors during a limit check, return 503 instead of allowing the request.
         * Production: set false so Redis outages do not multiply effective quotas across abuse paths.
         */
        val redisFailOpen: Boolean = true,
        val requestsPerMinute: Long = 100,
        /** Admin API: higher limit per IP (no full bypass). Default 200/min. */
        val adminRequestsPerMinute: Long = 200,
        /**
         * Session maintenance: `/auth/me`, `/auth/refresh`. Uses a dedicated bucket so heavy admin UI traffic
         * does not exhaust the general limit and block refresh (which surfaces as false "session expired").
         */
        val sessionRequestsPerMinute: Long = 300,
        /** Auth endpoints (login, register, passwordless): strict limit to prevent brute force. Default 5/min. */
        val authRequestsPerMinute: Long = 5,
        val storyGenerationFreePerHour: Int = 10,
        val storyGenerationPaidPerHour: Int = 60,
        val storyGenerationSuspiciousPerHour: Int = 2
    )

    data class JwtProperties(
        val secret: String = "",
        val accessExpirationMs: Long = 900000,
        val refreshExpirationMs: Long = 604800000
    )

    data class OpenAIProperties(
        val apiKey: String = "",
        val baseUrl: String = "https://api.openai.com",
        val model: String = "gpt-4o-mini",
        val maxTokens: Int = 1024,
        val connectTimeoutMs: Long = 15000,
        val readTimeoutMs: Long = 180000,
        /**
         * When true, [com.tamixa.infrastructure.openai.OpenAIClient.getModerationResult] fails closed if the API key is blank
         * (unsafe for children). Default false in dev; enable via `OPENAI_MODERATION_REQUIRED` or prod profile defaults.
         */
        val moderationRequired: Boolean = false,
        val retry: RetryProperties = RetryProperties()
    ) {
        data class RetryProperties(
            val maxAttempts: Int = 3,
            val initialIntervalMs: Long = 1000,
            val multiplier: Double = 2.0
        )
    }

    data class LlmProperties(
        /** openai (default) | gemini */
        val provider: String = "openai",
        val gemini: GeminiLlmProperties = GeminiLlmProperties(),
    ) {
        data class GeminiLlmProperties(
            val apiKey: String = "",
            val baseUrl: String = "https://generativelanguage.googleapis.com",
            val model: String = "gemini-2.0-flash",
            val connectTimeoutMs: Long = 15000,
            val readTimeoutMs: Long = 180000,
            /**
             * When true, [com.tamixa.infrastructure.gemini.GeminiLlmClient.getModerationResult] fails closed if the API key is blank.
             */
            val moderationRequired: Boolean = false,
        )
    }

    data class ImageGenerationProperties(
        /** openai = DALL·E 3 (default); gemini = Gemini image model ([generateContent] IMAGE modality). */
        val provider: String = "openai",
        val gemini: GeminiCoverImageProperties = GeminiCoverImageProperties(),
    ) {
        data class GeminiCoverImageProperties(
            val model: String = "gemini-2.5-flash-image",
            val aspectRatio: String = "1:1",
            val imageSize: String = "1K",
            /**
             * When false, omit [imageConfig] in the request (only `responseModalities`).
             * Use if a model rejects `imageConfig` or for troubleshooting.
             */
            val useImageConfig: Boolean = true,
        )
    }

    data class CoverAnimationProperties(
        val convertMp4ToGif: Boolean = true,
        val veo: VeoCoverAnimationProperties = VeoCoverAnimationProperties(),
    ) {
        /**
         * Google Veo 3.1 Lite via Gemini API ([predictLongRunning](https://ai.google.dev/gemini-api/docs/video)).
         * Uses [LlmProperties.GeminiLlmProperties.apiKey] (same as GEMINI_API_KEY) when enabled.
         */
        data class VeoCoverAnimationProperties(
            val enabled: Boolean = false,
            val baseUrl: String = "https://generativelanguage.googleapis.com",
            val model: String = "veo-3.1-lite-generate-preview",
            /** "4", "6", or "8" (shorter = lower cost). */
            val durationSeconds: String = "4",
            val resolution: String = "720p",
            val aspectRatio: String = "16:9",
            /** Image-to-video requires allow_adult per Google policy table. */
            val personGeneration: String = "allow_adult",
            val pollIntervalSec: Long = 10,
            val maxWaitSec: Long = 360,
        )
    }

    data class StoryProperties(
        val cacheTtlHours: Long = 24,
        /** Upper bound for library + pipeline text (translations / rewrite); aligns with STORY_MAX_WORDS default 900. */
        val maxWords: Int = 900,
        val themeAllowlist: String = "",
        val safetyScoreThreshold: Int = 60,
        val keywordBlocklist: String = "",
        /**
         * When true, parent-generated stories stop at [com.tamixa.domain.StoryStatus.PENDING_REVIEW] until an admin
         * approves in Story moderation; then status becomes PENDING and narration/cover jobs run.
         */
        val humanReviewBeforeNarration: Boolean = false
    )

    data class AiTokenLimitProperties(
        val perParentDaily: Int = 50000,
        val systemWideDaily: Int = 500000,
        val circuitBreakerSpikeThreshold: Int = 100000
    )

    data class TranslationPipelineProperties(
        /** Source language for curated stories (e.g. ta=Tamil). Translation skipped when target == source. */
        val sourceLanguage: String = "ta",
        val targetLanguages: String = "en,hi,te,kn,ml",  // comma-separated, parsed at runtime
        /** Base timeout; non-Tamil gets 1.5x in TranslationService. */
        val translationTimeoutMs: Long = 60000,
        val ttsTimeoutMs: Long = 90000,
        val maxRetries: Int = 3,
        val cacheTtlHours: Long = 24,
        val parallelism: Int = 4,
        val simulatedDelayMs: Long = 200,
        /** Per-language timeout (minutes). If a language exceeds this, it's marked failed and others continue. */
        val languageTimeoutMinutes: Int = 5,
        /** Total pipeline timeout (minutes) when processing all languages in parallel. Prevents indefinite hangs; increase if languages frequently timeout (e.g. 60–90 for 6 langs under load). */
        val totalTimeoutMinutes: Int = 60,
        /** When true (default), pipeline runs only when "Submit for review" is used. */
        val pipelineOnSubmitOnly: Boolean = true,
        /** When true (default): Submit for review only saves content; edit-page rebuild does translate+rewrite only (no TTS on submit). When false: full pipeline can run on submit. */
        val audioAfterApproval: Boolean = true,
        /** When true: Approve for delivery also starts background TTS if audio is still needed. When false (default): ops must use Narration (Story to Speech) → Generate audio after approval. */
        val autoTtsOnApprove: Boolean = false,
        /** TTS thread pool size for pipeline. Was 1 (bottleneck); 4 allows ~4x faster multi-story throughput. Stays under app.narration.max-concurrent-tts. */
        val ttsPoolSize: Int = 4,
        /** Rewrite (OpenAI) step timeout. Prevents indefinite hang when API is slow/unresponsive. 1 min fails fast. */
        val rewriteTimeoutMinutes: Int = 1,
        /** Max minutes a pipeline claim can block retries. After this, "Run pipeline" or Retry can take over (no need to click Clear stuck). */
        val claimMaxAgeMinutes: Int = 5,
        /**
         * When true, published stories keep `narrationApprovedAt` on metadata-only edits (theme/category/cover/age/childName/emotion).
         * Approval is still cleared when source or translation text changes.
         * When false (default), any save with status=PUBLISHED starts a fresh review cycle.
         */
        val keepNarrationApprovalOnMetadataOnlyPublishedUpdate: Boolean = false,
        /**
         * When true: translate/rewrite/TTS run only for each story's master language (`library_stories.language`).
         * Other locales skip the narration pipeline; playback reuses master narration audio when present.
         * Parent approved-library queries use master narration readiness instead of per-translation audio.
         */
        val masterOnlyNarration: Boolean = false
    )

    data class AudioProperties(
        val baseUrl: String = "/audio",
        val simulatedTtsDelayMs: Long = 500,
        /** When CDN disabled, used to build full URLs for stream-url fallback. */
        val publicBaseUrl: String = "http://localhost:8080",
        /**
         * Phase 4: optional short host / brand clip (MP4 URL). When non-blank, included in stream-url
         * responses for the app to show muted alongside story audio. Use https or a path joined with [publicBaseUrl].
         */
        val hostStoryClipUrl: String = ""
    )

    /** CDN/signed URLs: S3 presigned URLs (10 min expiry). Not a data class: includes derived [effectiveS3Bucket]. */
    class StorageProperties(
        val type: String = "s3",
        val s3Bucket: String = "tamixa-audio",
        val s3Region: String = "us-east-1"
    ) {
        val effectiveS3Bucket: String get() = s3Bucket.ifBlank { "tamixa-audio" }
    }

    data class CdnStreamProperties(
        val enabled: Boolean = false,
        /** Signed URL validity in minutes (5-10 recommended for S3 presigned) */
        val signedUrlExpiryMinutes: Long = 10,
        /** Cache TTL for CDN (seconds) */
        val cacheTtlSeconds: Long = 86400,
        /** Regex: paths under stories with a pending segment should not be CDN-cached. */
        val noCachePattern: String = "/stories/" + ".*" + "/pending/"
    )

    data class VoiceProperties(
        val encryptionKey: String = "",
        val maxFileSizeBytes: Long = 10485760
    )

    data class SubscriptionProperties(
        val freeStoriesPerMonth: Int = 5,
        val starterStoriesPerMonth: Int = 15,
        val freeVoiceGenerationsPerMonth: Int = 5,
        val trialDays: Int = 7,
        val gracePeriodDays: Int = 3,
        val webhookPayloadEncryptionKey: String = "",
        /** When true (e.g. in prod), startup fails if Stripe is enabled but webhook payload encryption key is not set. */
        val requireWebhookPayloadEncryption: Boolean = false,
        val gracePeriodCron: String = "0 0 2 * * ?",
        val trialExpiryCron: String = "0 0 3 * * ?",
        val stripe: StripeProperties = StripeProperties(),
        val zoho: ZohoProperties = ZohoProperties()
    ) {
        data class StripeProperties(
            val enabled: Boolean = false,
            val secretKey: String = "",
            val webhookSecret: String = "",
            val publishableKey: String = "",
            val successUrlPrefix: String = "https://app.tamixa.com/subscription",
            val voicePremiumPriceId: String = ""
        )

        data class ZohoProperties(
            val enabled: Boolean = false,
            val accountId: String = "",
            val webhookSigningKey: String = ""
        )
    }
}

/** Resolved absolute URL for optional host story clip in API responses, or null when unset. */
fun AppProperties.resolvedHostStoryClipUrl(): String? {
    val raw = audio.hostStoryClipUrl.trim()
    if (raw.isEmpty()) return null
    return when {
        raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true) -> raw
        else -> {
            val base = audio.publicBaseUrl.trimEnd('/')
            val path = if (raw.startsWith("/")) raw else "/$raw"
            "$base$path"
        }
    }
}
