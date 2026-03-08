package com.araro.infrastructure.pipeline

import com.araro.application.narration.StoryProcessingService
import com.araro.application.port.CuratedStoryEventPublisherPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * Triggers production narration pipeline when curated story is created.
 * Defers until AFTER the create transaction commits so findById sees the new row.
 * Uses triggerPipelineExecutor (not @Async) so pipeline reliably starts.
 */
@Component
@Primary
@Profile("!test")
class InlineCuratedStoryEventPublisher(
    private val storyProcessingService: StoryProcessingService,
    @Qualifier("triggerPipelineExecutor") private val triggerPipelineExecutor: TaskExecutor
) : CuratedStoryEventPublisherPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun publishCuratedStoryCreated(curatedStoryId: Long, content: String) {
        try {
            val id = curatedStoryId
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
            log.error("Curated story pipeline trigger failed for id={}", curatedStoryId, e)
        }
    }

    private fun schedulePipeline(masterStoryId: Long) {
        triggerPipelineExecutor.execute {
            try {
                storyProcessingService.processSync(masterStoryId)
                log.info("Create pipeline completed for story id={}", masterStoryId)
            } catch (e: Exception) {
                log.error("Create pipeline failed for story id={}: {}", masterStoryId, e.message, e)
            }
        }
    }
}
