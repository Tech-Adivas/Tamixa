package com.tamixa.application.narration

import com.tamixa.application.port.StoryTranslationRepositoryPort
import com.tamixa.domain.TranslationPipelineStatus
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Reprocess failed story translations every 30 minutes.
 * Respects max retry count. Paged to avoid loading unbounded rows into memory.
 */
@Component
class RetryFailedStoriesJob(
    private val translationRepository: StoryTranslationRepositoryPort,
    private val storyProcessingService: StoryProcessingService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val maxRetries = 3
    private val pageSize = 100

    @Scheduled(cron = "0 */30 * * * *")
    @SchedulerLock(name = "RetryFailedStoriesJob.retryFailedStories", lockAtMostFor = "PT30M", lockAtLeastFor = "PT20S")
    fun retryFailedStories() {
        val statuses = listOf(
            TranslationPipelineStatus.REWRITING,
            TranslationPipelineStatus.TRANSLATION_FAILED,
            TranslationPipelineStatus.REWRITE_FAILED,
            TranslationPipelineStatus.TTS_FAILED
        )
        var pageIdx = 0
        val processedMasters = mutableSetOf<Long>()
        var totalRows = 0
        while (true) {
            val page = translationRepository.findRetryableByStatusIn(
                statuses,
                maxRetries,
                PageRequest.of(pageIdx, pageSize)
            )
            if (page.isEmpty) break
            totalRows += page.numberOfElements
            for (t in page.content) {
                if (!processedMasters.add(t.masterStoryId)) continue
                try {
                    storyProcessingService.retryFailed(t.masterStoryId)
                } catch (e: Exception) {
                    log.error("Retry failed for masterStoryId={}", t.masterStoryId, e)
                }
            }
            if (!page.hasNext()) break
            pageIdx++
        }
        if (totalRows > 0) {
            log.info("RetryFailedStoriesJob: scanned {} retryable translation row(s), {} distinct master story id(s)", totalRows, processedMasters.size)
        }
    }
}
