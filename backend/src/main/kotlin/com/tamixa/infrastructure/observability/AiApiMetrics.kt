package com.tamixa.infrastructure.observability

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

/**
 * Per-API usage metrics for AI services (LLM, TTS, avatar video).
 * Used by admin AI metrics endpoint to show breakdown by API.
 */
@Component
class AiApiMetrics(private val registry: MeterRegistry) {

    fun recordGoogleTts(requests: Int = 1, characters: Int = 0) {
        if (requests > 0) registry.counter("ai.google_tts.requests").increment(requests.toDouble())
        if (characters > 0) registry.counter("ai.google_tts.characters").increment(characters.toDouble())
    }

    fun recordOpenaiTts(requests: Int = 1, characters: Int = 0) {
        if (requests > 0) registry.counter("ai.openai_tts.requests").increment(requests.toDouble())
        if (characters > 0) registry.counter("ai.openai_tts.characters").increment(characters.toDouble())
    }

    fun recordHeyGenAvatar() = registry.counter("ai.heygen_avatar.requests").increment()
    fun recordReplicateAvatar() = registry.counter("ai.replicate_avatar.requests").increment()
    fun recordDidAvatar() = registry.counter("ai.did_avatar.requests").increment()
    fun recordGooeyAvatar() = registry.counter("ai.gooey_avatar.requests").increment()
}
