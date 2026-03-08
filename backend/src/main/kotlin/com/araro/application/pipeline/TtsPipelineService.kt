package com.araro.application.pipeline

import com.araro.application.port.CuratedStoryRepositoryPort
import com.araro.application.port.StoryAudioRepositoryPort
import com.araro.application.port.StoryProcessingStatusRepositoryPort
import com.araro.application.port.TtsMetadataCachePort
import com.araro.domain.ProcessingStage
import com.araro.domain.StoryAudio
import com.araro.infrastructure.config.AppProperties
import com.araro.infrastructure.observability.StoryPipelineMetrics
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Runs TTS pipeline in-process (replaces Kafka TtsRequestListener).
 * Idempotent: skips if story_audio exists.
 */
@Service
class TtsPipelineService(
    private val curatedStoryRepository: CuratedStoryRepositoryPort,
    private val storyAudioRepository: StoryAudioRepositoryPort,
    private val ttsMetadataCache: TtsMetadataCachePort,
    private val processingStatusRepository: StoryProcessingStatusRepositoryPort,
    private val appProperties: AppProperties,
    private val pipelineMetrics: StoryPipelineMetrics,
    @Value("\${app.audio.base-url:/audio}") private val audioBaseUrl: String,
    @Value("\${app.audio.simulated-tts-delay-ms:500}") private val simulatedTtsDelayMs: Long
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val maxRetries get() = appProperties.translationPipeline.maxRetries

    data class TtsRequest(val masterStoryId: Long, val language: String, val content: String)

    @Transactional
    fun runTts(request: TtsRequest) {
        if (storyAudioRepository.existsByMasterStoryIdAndLanguage(request.masterStoryId, request.language)) {
            log.debug("TTS exists masterId={} lang={}, idempotent skip", request.masterStoryId, request.language)
            processingStatusRepository.updateStage(request.masterStoryId, request.language, ProcessingStage.READY, null)
            return
        }

        val cachedUrl = ttsMetadataCache.getAudioUrl(request.masterStoryId, request.language)
        if (cachedUrl != null) {
            log.debug("TTS cache hit masterId={} lang={}", request.masterStoryId, request.language)
            saveAudioIfNeeded(request.masterStoryId, request.language, cachedUrl)
            processingStatusRepository.updateStage(request.masterStoryId, request.language, ProcessingStage.READY, null)
            return
        }

        val status = processingStatusRepository.findByMasterStoryIdAndLanguage(request.masterStoryId, request.language)
        if (status != null && status.retryCount >= maxRetries) {
            log.warn("Max retries reached for TTS masterId={} lang={}, skipping", request.masterStoryId, request.language)
            processingStatusRepository.updateStage(request.masterStoryId, request.language, ProcessingStage.FAILED, "Max retries exceeded")
            pipelineMetrics.recordTtsFailure()
            return
        }

        try {
            pipelineMetrics.recordTtsLatency {
                simulateTextToAudio(request.masterStoryId, request.content)
                val audioFileUrl = "$audioBaseUrl/curated/${request.masterStoryId}_${request.language}.wav"
                saveAudioIfNeeded(request.masterStoryId, request.language, audioFileUrl)
                ttsMetadataCache.setAudioUrl(request.masterStoryId, request.language, audioFileUrl)
                processingStatusRepository.updateStage(request.masterStoryId, request.language, ProcessingStage.READY, null)
                curatedStoryRepository.findById(request.masterStoryId)?.let { master ->
                    val readyTimeMs = java.time.Duration.between(master.createdAt, Instant.now()).toMillis()
                    pipelineMetrics.recordStoryReadyTime(readyTimeMs)
                }
                pipelineMetrics.recordTtsSuccess()
            }
        } catch (e: Exception) {
            log.error("TTS failed masterId={} lang={}", request.masterStoryId, request.language, e)
            processingStatusRepository.incrementRetryCount(request.masterStoryId, request.language, e.message ?: "Unknown error")
            pipelineMetrics.recordTtsFailure()
            throw e
        }
    }

    private fun saveAudioIfNeeded(masterStoryId: Long, language: String, audioFileUrl: String) {
        if (storyAudioRepository.existsByMasterStoryIdAndLanguage(masterStoryId, language)) return
        val audio = StoryAudio(
            id = 0,
            masterStoryId = masterStoryId,
            language = language,
            audioFileUrl = audioFileUrl,
            createdAt = Instant.now()
        )
        storyAudioRepository.save(audio)
    }

    private fun simulateTextToAudio(masterStoryId: Long, content: String) {
        if (simulatedTtsDelayMs > 0) Thread.sleep(simulatedTtsDelayMs)
        log.debug("Simulated TTS for masterId={} contentLength={}", masterStoryId, content.length)
    }
}
