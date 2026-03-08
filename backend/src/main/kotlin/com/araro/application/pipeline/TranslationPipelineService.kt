package com.araro.application.pipeline

import com.araro.application.narration.EmotionToToneMapper
import com.araro.application.port.CuratedStoryRepositoryPort
import com.araro.application.port.StoryPipelineEventPublisherPort
import com.araro.application.port.StoryProcessingStatusRepositoryPort
import com.araro.application.port.StoryTranslationRepositoryPort
import com.araro.application.port.TranslationCachePort
import com.araro.application.translation.TranslationService
import com.araro.domain.ProcessingStage
import com.araro.domain.StoryTranslation
import com.araro.domain.narration.ToneMode
import com.araro.infrastructure.config.AppProperties
import com.araro.infrastructure.observability.StoryPipelineMetrics
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Runs translation pipeline in-process (replaces Kafka TranslationRequestListener).
 * Idempotent: skips if story_translations already exists.
 */
@Service
class TranslationPipelineService(
    private val translationService: TranslationService,
    private val translationRepository: StoryTranslationRepositoryPort,
    private val curatedStoryRepository: CuratedStoryRepositoryPort,
    private val translationCache: TranslationCachePort,
    private val processingStatusRepository: StoryProcessingStatusRepositoryPort,
    @Lazy private val pipelinePublisher: StoryPipelineEventPublisherPort,
    private val appProperties: AppProperties,
    private val pipelineMetrics: StoryPipelineMetrics
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val maxRetries get() = appProperties.translationPipeline.maxRetries

    data class TranslationRequest(
        val masterStoryId: Long,
        val language: String,
        val sourceContent: String,
        val sourceTitle: String?,
        val sourceMoral: String?
    )

    @Transactional
    fun runTranslation(request: TranslationRequest) {
        val toneMode = resolveToneMode(request.masterStoryId)
        if (translationRepository.existsByMasterStoryIdAndLanguage(request.masterStoryId, request.language)) {
            log.debug("Translation exists masterId={} lang={}, idempotent skip", request.masterStoryId, request.language)
            val existing = translationRepository.findByMasterStoryIdAndLanguage(request.masterStoryId, request.language)!!
            pipelinePublisher.publishTtsRequest(request.masterStoryId, request.language, existing.content)
            pipelinePublisher.publishNarrationRequest(existing.id, toneMode, listOf("default"), null)
            return
        }

        val cached = translationCache.get(request.masterStoryId, request.language)
        if (cached != null) {
            log.debug("Translation cache hit masterId={} lang={}", request.masterStoryId, request.language)
            if (!translationRepository.existsByMasterStoryIdAndLanguage(request.masterStoryId, request.language)) {
                saveTranslationAndPublish(request, cached.title, cached.content, cached.moral, cached.wordCount, cached.readingTimeMinutes)
            } else {
                val existing = translationRepository.findByMasterStoryIdAndLanguage(request.masterStoryId, request.language)!!
                pipelinePublisher.publishTtsRequest(request.masterStoryId, request.language, existing.content)
                pipelinePublisher.publishNarrationRequest(existing.id, toneMode, listOf("default"), null)
            }
            processingStatusRepository.updateStage(request.masterStoryId, request.language, ProcessingStage.TTS_GENERATING, null)
            return
        }

        val status = processingStatusRepository.findByMasterStoryIdAndLanguage(request.masterStoryId, request.language)
        if (status != null && status.retryCount >= maxRetries) {
            log.warn("Max retries reached masterId={} lang={}, skipping", request.masterStoryId, request.language)
            processingStatusRepository.updateStage(request.masterStoryId, request.language, ProcessingStage.FAILED, "Max retries exceeded")
            pipelineMetrics.recordTranslationFailure()
            return
        }

        try {
            pipelineMetrics.recordTranslationLatency {
                val translated = translationService.translateIfNeeded(
                    sourceLang = appProperties.translationPipeline.sourceLanguage,
                    targetLang = request.language,
                    title = request.sourceTitle,
                    content = request.sourceContent,
                    moral = request.sourceMoral
                )
                val wordCount = translated.content.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                val readingTimeMinutes = (wordCount / 150.0).coerceAtMost(5.0)

                if (translated.translated) {
                    translationCache.set(
                        request.masterStoryId,
                        request.language,
                        com.araro.application.port.CachedTranslation(
                            title = translated.title,
                            content = translated.content,
                            moral = translated.moral,
                            wordCount = wordCount,
                            readingTimeMinutes = readingTimeMinutes
                        )
                    )
                }
                saveTranslationAndPublish(request, translated.title, translated.content, translated.moral, wordCount, readingTimeMinutes)
                processingStatusRepository.updateStage(request.masterStoryId, request.language, ProcessingStage.TTS_GENERATING, null)
                pipelineMetrics.recordTranslationSuccess()
            }
        } catch (e: Exception) {
            log.error("Translation failed masterId={} lang={}", request.masterStoryId, request.language, e)
            processingStatusRepository.incrementRetryCount(request.masterStoryId, request.language, e.message ?: "Unknown error")
            pipelineMetrics.recordTranslationFailure()
            throw e
        }
    }

    private fun saveTranslationAndPublish(
        request: TranslationRequest,
        title: String?,
        content: String,
        moral: String?,
        wordCount: Int,
        readingTimeMinutes: Double
    ) {
        val translation = StoryTranslation(
            id = 0,
            masterStoryId = request.masterStoryId,
            language = request.language,
            title = title,
            content = content,
            moral = moral,
            wordCount = wordCount,
            readingTimeMinutes = readingTimeMinutes,
            createdAt = Instant.now()
        )
        val saved = translationRepository.save(translation)
        val toneMode = resolveToneMode(request.masterStoryId)
        pipelinePublisher.publishTtsRequest(request.masterStoryId, request.language, content)
        pipelinePublisher.publishNarrationRequest(saved.id, toneMode, listOf("default"), null)
    }

    private fun resolveToneMode(masterStoryId: Long): ToneMode {
        val story = curatedStoryRepository.findById(masterStoryId) ?: return ToneMode.CALM
        return EmotionToToneMapper.toToneMode(story.emotionMode)
    }
}
