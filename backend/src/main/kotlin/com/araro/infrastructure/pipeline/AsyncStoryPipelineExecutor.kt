package com.araro.infrastructure.pipeline

import com.araro.application.narration.StoryProcessingOrchestrator
import com.araro.application.pipeline.TranslationPipelineService
import com.araro.application.pipeline.TtsPipelineService
import com.araro.application.port.StoryTranslationRepositoryPort
import com.araro.application.port.StoryPipelineEventPublisherPort
import com.araro.domain.narration.ToneMode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.concurrent.ExecutorService

/**
 * In-process story pipeline executor. Replaces Kafka for Beta simplification.
 * Uses ThreadPoolTaskExecutor for async processing; no external message broker.
 */
@Component
@Primary
@Profile("!test")
class AsyncStoryPipelineExecutor(
    private val translationPipelineService: TranslationPipelineService,
    private val ttsPipelineService: TtsPipelineService,
    private val orchestrator: StoryProcessingOrchestrator,
    private val translationRepository: StoryTranslationRepositoryPort,
    @Qualifier("narrationTtsExecutor") private val narrationTtsExecutor: ExecutorService
) : StoryPipelineEventPublisherPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun publishTranslationRequest(
        masterStoryId: Long,
        language: String,
        sourceContent: String,
        sourceTitle: String?,
        sourceMoral: String?
    ) {
        narrationTtsExecutor.execute {
            try {
                translationPipelineService.runTranslation(
                    TranslationPipelineService.TranslationRequest(
                        masterStoryId = masterStoryId,
                        language = language,
                        sourceContent = sourceContent,
                        sourceTitle = sourceTitle,
                        sourceMoral = sourceMoral
                    )
                )
            } catch (e: Exception) {
                log.error("Translation pipeline failed masterId={} lang={}", masterStoryId, language, e)
            }
        }
    }

    override fun publishTtsRequest(masterStoryId: Long, language: String, content: String) {
        narrationTtsExecutor.execute {
            try {
                ttsPipelineService.runTts(
                    TtsPipelineService.TtsRequest(masterStoryId = masterStoryId, language = language, content = content)
                )
            } catch (e: Exception) {
                log.error("TTS pipeline failed masterId={} lang={}", masterStoryId, language, e)
            }
        }
    }

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
