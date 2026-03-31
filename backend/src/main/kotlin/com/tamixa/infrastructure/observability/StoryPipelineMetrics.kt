package com.tamixa.infrastructure.observability

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component

/**
 * Metrics for translation and TTS pipeline.
 * Exposed via /actuator/prometheus: translation_latency, tts_latency,
 * translation_failure_rate, tts_failure_rate, story_ready_time.
 */
@Component
class StoryPipelineMetrics(
    private val registry: MeterRegistry
) {
    private val translationLatency: Timer = registry.timer("translation_latency")
    private val ttsLatency: Timer = registry.timer("tts_latency")
    private val storyReadyTimer: Timer = registry.timer("story_ready_time")
    private val translationSuccess: Counter = registry.counter("translation_success_count")
    private val translationFailure: Counter = registry.counter("translation_failure_count")
    private val ttsSuccess: Counter = registry.counter("tts_success_count")
    private val ttsFailure: Counter = registry.counter("tts_failure_count")
    private val dltTranslationCount: Counter = registry.counter("translation_dlt_count")
    private val dltTtsCount: Counter = registry.counter("tts_dlt_count")

    fun recordTranslationDlt() = dltTranslationCount.increment()
    fun recordTtsDlt() = dltTtsCount.increment()

    fun <T> recordTranslationLatency(block: () -> T): T {
        val sample = Timer.start(registry)
        return try {
            block()
        } finally {
            sample.stop(translationLatency)
        }
    }

    fun <T> recordTtsLatency(block: () -> T): T {
        val sample = Timer.start(registry)
        return try {
            block()
        } finally {
            sample.stop(ttsLatency)
        }
    }

    fun recordStoryReadyTime(readyTimeMs: Long) {
        storyReadyTimer.record(readyTimeMs, java.util.concurrent.TimeUnit.MILLISECONDS)
    }

    fun recordTranslationSuccess() = translationSuccess.increment()
    fun recordTranslationFailure() = translationFailure.increment()
    fun recordTtsSuccess() = ttsSuccess.increment()
    fun recordTtsFailure() = ttsFailure.increment()

    /**
     * Admin curated-story update latency. Tag by [scope] ("service" | "controller"), [outcome] ("success" | "error"),
     * and requested/effective [status] to inspect p95/p99 for submit-for-review vs draft saves.
     */
    fun recordAdminStoryUpdateLatency(durationMs: Long, scope: String, outcome: String, status: String) {
        val safeScope = scope.ifBlank { "unknown" }.take(20)
        val safeOutcome = outcome.ifBlank { "unknown" }.take(20)
        val safeStatus = status.ifBlank { "unknown" }.take(20).uppercase()
        registry.timer(
            "admin_story_update_latency",
            "scope", safeScope,
            "outcome", safeOutcome,
            "status", safeStatus
        ).record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS)
    }

    /** Translation failure rate = translation_failure_count / (success + failure). Computed in Prometheus. */
    /** TTS failure rate = tts_failure_count / (success + failure). Computed in Prometheus. */
}
