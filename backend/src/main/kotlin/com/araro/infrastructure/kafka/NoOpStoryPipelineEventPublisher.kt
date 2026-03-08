package com.araro.infrastructure.kafka

import com.araro.application.port.StoryPipelineEventPublisherPort
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("test")
class NoOpStoryPipelineEventPublisher : StoryPipelineEventPublisherPort {

    override fun publishTranslationRequest(
        masterStoryId: Long,
        language: String,
        sourceContent: String,
        sourceTitle: String?,
        sourceMoral: String?
    ) {}

    override fun publishTtsRequest(masterStoryId: Long, language: String, content: String) {}

    override fun publishNarrationRequest(translationId: Long, toneMode: com.araro.domain.narration.ToneMode?, voiceProfiles: List<String>?, parentId: Long?) {}
}
