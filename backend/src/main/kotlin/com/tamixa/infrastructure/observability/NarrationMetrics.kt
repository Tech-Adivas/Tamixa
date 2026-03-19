package com.tamixa.infrastructure.observability

import com.tamixa.application.narration.NarrationConcurrencyLimiter
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Micrometer metrics for the Conversational Narration Engine.
 * Cost monitoring: AI token usage, TTS characters processed.
 * Exposed via /actuator/prometheus and /actuator/metrics.
 *
 * Metrics:
 * - narration_latency, tts_latency, emotion_tagging_latency: timing
 * - narration_failure_rate, tts_failure_count: failures
 * - safety_validation_failures: validation rejects
 * - narration_ai_tokens_total: daily AI token usage
 * - narration_tts_characters_total: TTS input character count
 * - premium_voice_usage: premium voice stream requests
 * - tts_concurrency_current: current in-flight TTS jobs (scale monitoring)
 */
@Component
class NarrationMetrics(
    private val registry: MeterRegistry,
    @Autowired(required = false) concurrencyLimiter: NarrationConcurrencyLimiter? = null
) {
    private val narrationLatency: Timer = registry.timer("narration_latency")
    private val ttsLatency: Timer = registry.timer("tts_latency")
    private val emotionTaggingLatency: Timer = registry.timer("emotion_tagging_latency")
    private val narrationFailure: Counter = registry.counter("narration_failure_rate")
    private val ttsFailure: Counter = registry.counter("tts_failure_count")
    private val safetyValidationFailures: Counter = registry.counter("safety_validation_failures")
    private val dailyTokenUsage: Counter = registry.counter("narration_ai_tokens_total")
    private val ttsCharactersProcessed: Counter = registry.counter("narration_tts_characters_total")
    private val premiumVoiceUsage: Counter = registry.counter("premium_voice_usage")

    init {
        concurrencyLimiter?.let { limiter ->
            Gauge.builder("tts_concurrency_current", limiter) { it.currentInUse().toDouble() }
                .description("Current concurrent TTS narration jobs")
                .register(registry)
        }
    }

    fun <T> recordNarrationLatency(block: () -> T): T {
        return narrationLatency.recordCallable(block) ?: throw IllegalStateException("Narration block returned null")
    }

    fun recordTtsLatency(block: () -> ByteArray?): ByteArray? {
        val sample = Timer.start(registry)
        return try {
            block()
        } finally {
            sample.stop(ttsLatency)
        }
    }

    fun recordNarrationFailure() = narrationFailure.increment()
    fun recordTtsFailure() = ttsFailure.increment()
    fun recordSafetyValidationFailure() = safetyValidationFailures.increment()

    /** Track AI token usage (prompt + completion) for cost monitoring. */
    fun recordTokenUsage(tokens: Int) = dailyTokenUsage.increment(tokens.toDouble())

    /** Track TTS characters processed (SSML length) for cost monitoring. */
    fun recordTtsCharacters(count: Int) = ttsCharactersProcessed.increment(count.toDouble())

    /** Track emotion tagging duration (deterministic layer). */
    fun <T> recordEmotionTaggingLatency(block: () -> T): T =
        emotionTaggingLatency.recordCallable(block) ?: throw IllegalStateException("Emotion tagging returned null")

    /** Track premium voice stream access for monetization analytics. */
    fun recordPremiumVoiceUsage() = premiumVoiceUsage.increment()
}
