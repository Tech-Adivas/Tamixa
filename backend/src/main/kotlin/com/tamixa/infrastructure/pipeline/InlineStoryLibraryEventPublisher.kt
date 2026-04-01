package com.tamixa.infrastructure.pipeline

import com.tamixa.application.port.StoryLibraryEventPublisherPort
import com.tamixa.application.port.StoryPipelineTriggerPort
import com.tamixa.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * Triggers production narration pipeline when library story is created.
 * When pipelineOnSubmitOnly=true (default), does NOT run pipeline on create;
 * pipeline runs only after "Approve for delivery" in Story for review.
 * Defers until AFTER the create transaction commits so findById sees the new row.
 * Uses triggerPipelineExecutor (not @Async) so pipeline reliably starts.
 */
@Component
@Primary
@Profile("!test")
class InlineStoryLibraryEventPublisher(
    @Lazy private val storyPipelineTriggerPort: StoryPipelineTriggerPort,
    private val appProperties: AppProperties
) : StoryLibraryEventPublisherPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun publishLibraryStoryCreated(libraryStoryId: Long, content: String, status: String) {
        try {
            if (!status.equals("PUBLISHED", ignoreCase = true)) {
                log.info("Pipeline NOT triggered on create for non-published story: masterStoryId={} status={}", libraryStoryId, status)
                return
            }
            if (appProperties.translationPipeline.pipelineOnSubmitOnly) {
                log.info("Pipeline NOT triggered on create (pipelineOnSubmitOnly=true): masterStoryId={}. Pipeline runs only when Submit for review is used.", libraryStoryId)
                return
            }
            val id = libraryStoryId
            if (!TransactionSynchronizationManager.isSynchronizationActive()) {
                log.info("Pipeline trigger (no tx): masterStoryId={}", id)
                schedulePipeline(id)
                return
            }
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() {
                    log.info("Pipeline trigger (after commit): masterStoryId={}", id)
                    schedulePipeline(id)
                }
            })
        } catch (e: Exception) {
            log.error("Library story pipeline trigger failed for id={}", libraryStoryId, e)
        }
    }

    private fun schedulePipeline(masterStoryId: Long) {
        storyPipelineTriggerPort.scheduleProcessSync(masterStoryId, translationOnly = false)
        log.info("Create pipeline scheduled for story id={}", masterStoryId)
    }
}
