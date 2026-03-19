package com.tamixa.infrastructure.kafka

import com.tamixa.application.port.StoryEventPublisherPort
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("test")
class NoOpStoryEventPublisher : StoryEventPublisherPort {
    override fun publishStoryCreated(storyId: Long, content: String) {}
}
