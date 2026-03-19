package com.tamixa.infrastructure.kafka

import com.tamixa.application.port.StoryLibraryEventPublisherPort
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("test")
class NoOpStoryLibraryEventPublisher : StoryLibraryEventPublisherPort {

    override fun publishLibraryStoryCreated(libraryStoryId: Long, content: String) {
        // No-op in tests; Kafka is disabled
    }
}
