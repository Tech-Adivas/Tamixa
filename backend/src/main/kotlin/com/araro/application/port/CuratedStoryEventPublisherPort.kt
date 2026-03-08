package com.araro.application.port

/**
 * Publishes events for curated story audio generation.
 * Same pipeline as AI stories: text → Kafka → TTS → update audio URL.
 */
interface CuratedStoryEventPublisherPort {

    fun publishCuratedStoryCreated(curatedStoryId: Long, content: String)
}
