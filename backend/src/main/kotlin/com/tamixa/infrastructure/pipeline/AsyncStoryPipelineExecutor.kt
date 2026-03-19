package com.tamixa.infrastructure.pipeline

import com.tamixa.application.narration.StoryProcessingOrchestrator
import com.tamixa.application.port.StoryPipelineEventPublisherPort
import com.tamixa.application.port.StoryTranslationRepositoryPort
import com.tamixa.domain.narration.ToneMode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.concurrent.ExecutorService

/**
 * In-process narration pipeline executor for on-demand cloned-voice narration.
 * Curated story TTS uses StoryProcessingService (approve-narration flow).
 */
@Component
@Primary
@Profile("!test")
class AsyncStoryPipelineExecutor(
    private val orchestrator: StoryProcessingOrchestrator,
    private val translationRepository: StoryTranslationRepositoryPort,
    @Qualifier("narrationTtsExecutor") private val narrationTtsExecutor: ExecutorService
) : StoryPipelineEventPublisherPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun publishNarrationRequest(
        translationId: Long,
        toneMode: ToneMode?,
        voiceProfiles: List<String>?,
        parentId: Long?
    ) {
        narrationTtsExecutor.execute {
            try {
                val translation = translationRepository.findById(translationId)
                if (translation == null) {
                    log.warn("Translation id={} not found for narration, skipping", translationId)
                    return@execute
                }
                orchestrator.process(
                    translation = translation,
                    toneMode = toneMode ?: ToneMode.CALM,
                    voiceProfiles = voiceProfiles?.ifEmpty { listOf("default") } ?: listOf("default"),
                    parentId = parentId
                )
            } catch (e: Exception) {
                log.error("Narration processing failed for translationId={}", translationId, e)
            }
        }
    }
}
