package com.araro.infrastructure.observability

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * Production narration pipeline metrics.
 * - narration_latency, translation_latency, rewrite_latency, tts_latency
 * - narration_failure_count, daily_generation_count
 */
@Component
class NarrationPipelineMetrics(private val registry: MeterRegistry) {

    private val narrationLatency: Timer = registry.timer("narration_latency")
    private val translationLatency: Timer = registry.timer("translation_latency")
    private val rewriteLatency: Timer = registry.timer("rewrite_latency")
    private val ttsLatency: Timer = registry.timer("tts_latency")
    private val narrationFailureCount: Counter = registry.counter("narration_failure_count")
    private val dailyGenerationCount: Counter = registry.counter("daily_generation_count")

    fun recordNarrationLatency(nanos: Long) {
        narrationLatency.record(nanos, TimeUnit.NANOSECONDS)
    }

    fun <T> recordTranslationLatency(block: () -> T): T {
        val sample = Timer.start(registry)
        return try {
            block()
        } finally {
            sample.stop(translationLatency)
        }
    }

    fun <T> recordRewriteLatency(block: () -> T): T {
        val sample = Timer.start(registry)
        return try {
            block()
        } finally {
            sample.stop(rewriteLatency)
        }
    }

    fun recordTtsLatency(nanos: Long) {
        ttsLatency.record(nanos, TimeUnit.NANOSECONDS)
    }

    fun recordNarrationFailure() = narrationFailureCount.increment()

    fun recordDailyGeneration() = dailyGenerationCount.increment()
}
