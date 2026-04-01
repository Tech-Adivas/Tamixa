package com.tamixa.infrastructure.pipeline

import com.tamixa.application.narration.StoryProcessingService
import com.tamixa.application.port.StoryPipelineTriggerPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Component

@Component
class LocalStoryPipelineTriggerPort(
    private val storyProcessingService: StoryProcessingService,
    @Qualifier("triggerPipelineExecutor") private val triggerPipelineExecutor: TaskExecutor
) : StoryPipelineTriggerPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun scheduleProcessSync(masterStoryId: Long, translationOnly: Boolean) {
        triggerPipelineExecutor.execute {
            try {
                storyProcessingService.processSync(masterStoryId, translationOnly = translationOnly)
            } catch (e: Exception) {
                log.error("Background processSync failed masterStoryId={}", masterStoryId, e)
            }
        }
    }
}
