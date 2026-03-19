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
    val shareClip: ShareClipProperties = ShareClipProperties()
) {

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
        /** Provider: elevenlabs | google | xtts. Default from application.yml: google */
        val provider: String = "google",
        val elevenLabsApiKey: String = "",
        val elevenLabsBaseUrl: String = "https://api.elevenlabs.io",
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
        val processCron: String = "0 */2 * * * *"
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
        /** Comma-separated origins; empty or "*" = allow all (dev only). */
        val allowedOrigins: String = "*"
    )

    data class RateLimitProperties(
        val enabled: Boolean = true,
        /** When true, use Redis for rate limiting (multi-instance). Requires Redis. Default false. */
        val useRedis: Boolean = false,
        val requestsPerMinute: Long = 100,
        /** Admin API: higher limit per IP (no full bypass). Default 200/min. */
        val adminRequestsPerMinute: Long = 200,
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
        val retry: RetryProperties = RetryProperties()
    ) {
        data class RetryProperties(
            val maxAttempts: Int = 3,
            val initialIntervalMs: Long = 1000,
            val multiplier: Double = 2.0
        )
    }

    data class StoryProperties(
        val cacheTtlHours: Long = 24,
        val maxWords: Int = 750,
        val themeAllowlist: String = "",
        val safetyScoreThreshold: Int = 60,
        val keywordBlocklist: String = ""
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
        /** When true (default): Submit for review only saves content and does not trigger pipeline. Use "Story to Speech" after approval to generate audio. When false: pipeline runs on submit. */
        val audioAfterApproval: Boolean = true,
        /** TTS thread pool size for pipeline. Was 1 (bottleneck); 4 allows ~4x faster multi-story throughput. Stays under app.narration.max-concurrent-tts. */
        val ttsPoolSize: Int = 4,
        /** Rewrite (OpenAI) step timeout. Prevents indefinite hang when API is slow/unresponsive. 1 min fails fast. */
        val rewriteTimeoutMinutes: Int = 1,
        /** Max minutes a pipeline claim can block retries. After this, "Run pipeline" or Retry can take over (no need to click Clear stuck). */
        val claimMaxAgeMinutes: Int = 5
    )

    data class AudioProperties(
        val baseUrl: String = "/audio",
        val simulatedTtsDelayMs: Long = 500,
        /** When CDN disabled, used to build full URLs for stream-url fallback. */
        val publicBaseUrl: String = "http://localhost:8080"
    )

    /** CDN/signed URLs: S3 presigned URLs (10 min expiry). */
    data class StorageProperties(
        val type: String = "s3",
        val s3Bucket: String = "tamixa-audio",
        val s3Region: String = "us-east-1"
    ) {
        /** Effective S3 bucket: s3Bucket if set, else default tamixa-audio. Use everywhere instead of ifBlank fallback. */
        val effectiveS3Bucket: String get() = s3Bucket.ifBlank { "tamixa-audio" }
    }

    data class CdnStreamProperties(
        val enabled: Boolean = false,
        /** Signed URL validity in minutes (5-10 recommended for S3 presigned) */
        val signedUrlExpiryMinutes: Long = 10,
        /** Cache TTL for CDN (seconds) */
        val cacheTtlSeconds: Long = 86400,
        /** Don't cache paths matching PENDING indicator */
        val noCachePattern: String = "/stories/.*/pending/"
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
