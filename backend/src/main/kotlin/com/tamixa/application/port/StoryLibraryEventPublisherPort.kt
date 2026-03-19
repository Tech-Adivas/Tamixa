package com.tamixa.application.port

/**
 * Publishes events for library story audio generation.
 * Same pipeline as AI stories: text → Kafka → TTS → update audio URL.
 */
interface StoryLibraryEventPublisherPort {

    fun publishLibraryStoryCreated(libraryStoryId: Long, content: String)
}
