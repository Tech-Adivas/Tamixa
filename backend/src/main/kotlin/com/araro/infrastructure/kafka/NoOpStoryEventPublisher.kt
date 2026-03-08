package com.araro.infrastructure.kafka

import com.araro.application.port.StoryEventPublisherPort
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("test")
class NoOpStoryEventPublisher : StoryEventPublisherPort {
    override fun publishStoryCreated(storyId: Long, content: String) {}
}
