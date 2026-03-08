package com.araro.infrastructure.observability

import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component

/**
 * Performance metrics for audio streaming.
 * - stream_start_latency: time to generate signed URL
 * - stream_url_requests: counter of stream URL requests
 * - buffering_events / audio_completion_rate: client-side, reported via analytics API
 */
@Component
class StreamMetrics(private val registry: MeterRegistry) {

    private val urlGenerationTimer: Timer = Timer.builder("stream_url_generation_latency")
        .tag("component", "backend")
        .description("Time to generate signed stream URL")
        .register(registry)

    private val urlRequestsCounter = registry.counter("stream_url_requests", "component", "backend")

    fun recordStreamUrlGenerationLatency(nanos: Long) {
        urlGenerationTimer.record(java.time.Duration.ofNanos(nanos))
        urlRequestsCounter.increment()
    }

    private val streamStartLatencySummary: DistributionSummary = DistributionSummary
        .builder("stream_start_latency")
        .tag("component", "client")
        .description("Client-reported latency from request to first audio byte (ms)")
        .register(registry)

    /** Call when client reports stream started (latency from request to first byte). */
    fun recordStreamStartLatency(ms: Double) {
        streamStartLatencySummary.record(ms)
    }

    /** Call when client reports buffering event. */
    fun recordBufferingEvent(storyId: Long) {
        registry.counter("stream_buffering_events", "story_id", storyId.toString()).increment()
    }

    /** Call when client reports audio completed. */
    fun recordAudioCompletion(storyId: Long) {
        registry.counter("stream_audio_completions", "story_id", storyId.toString()).increment()
    }
}
