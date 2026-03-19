package com.tamixa.infrastructure.kafka

import com.tamixa.application.port.StoryPipelineEventPublisherPort
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("test")
class NoOpStoryPipelineEventPublisher : StoryPipelineEventPublisherPort {
    override fun publishNarrationRequest(translationId: Long, toneMode: com.tamixa.domain.narration.ToneMode?, voiceProfiles: List<String>?, parentId: Long?) {}
}
