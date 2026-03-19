package com.tamixa.application.narration

import com.tamixa.application.port.StoryTranslationRepositoryPort
import com.tamixa.domain.TranslationPipelineStatus
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Reprocess failed story translations every 30 minutes.
 * Respects max retry count. Logs failure count.
 */
@Component
class RetryFailedStoriesJob(
    private val translationRepository: StoryTranslationRepositoryPort,
    private val storyProcessingService: StoryProcessingService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val maxRetries = 3

    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    fun retryFailedStories() {
        val failed = translationRepository.findByStatusIn(
            listOf(
                TranslationPipelineStatus.REWRITING,       // stuck in-progress
                TranslationPipelineStatus.TRANSLATION_FAILED,
                TranslationPipelineStatus.REWRITE_FAILED,
                TranslationPipelineStatus.TTS_FAILED
            )
        ).filter { it.retryCount < maxRetries }

        if (failed.isEmpty()) return

        val failureCount = failed.size
        log.info("RetryFailedStoriesJob: reprocessing {} failed translations", failureCount)

        val masterStoryIds = failed.map { it.masterStoryId }.distinct()
        for (masterStoryId in masterStoryIds) {
            try {
                storyProcessingService.retryFailed(masterStoryId)
            } catch (e: Exception) {
                log.error("Retry failed for masterStoryId={}", masterStoryId, e)
            }
        }
    }
}
