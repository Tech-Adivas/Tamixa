package com.araro.infrastructure.kafka

import com.araro.application.port.CuratedStoryEventPublisherPort
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("test")
class NoOpCuratedStoryEventPublisher : CuratedStoryEventPublisherPort {

    override fun publishCuratedStoryCreated(curatedStoryId: Long, content: String) {
        // No-op in tests; Kafka is disabled
    }
}
