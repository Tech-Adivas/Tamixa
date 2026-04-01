package com.tamixa.application.port

/**
 * Phase 3 seam: enqueue full narration pipeline work without blocking the HTTP thread.
 * Today implemented by [com.tamixa.infrastructure.pipeline.LocalStoryPipelineTriggerPort] (in-process executor).
 * Future: remote story-orchestrator service (HTTP or message bus) behind the same port.
 */
fun interface StoryPipelineTriggerPort {

    /** Run [com.tamixa.application.narration.StoryProcessingService.processSync] in the background. */
    fun scheduleProcessSync(masterStoryId: Long, translationOnly: Boolean)
}
