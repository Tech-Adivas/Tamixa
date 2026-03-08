package com.araro.infrastructure.observability

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks which language is currently being processed per story.
 * Used by pipeline-status API to show "Generating X audio..." on frontend.
 */
@Component
class PipelineProgressTracker {

    private val activeLanguage = ConcurrentHashMap<Long, String>()

    fun setProcessing(masterStoryId: Long, language: String) {
        activeLanguage[masterStoryId] = language
    }

    fun clearProcessing(masterStoryId: Long) {
        activeLanguage.remove(masterStoryId)
    }

    fun getProcessingLanguage(masterStoryId: Long): String? =
        activeLanguage[masterStoryId]
}
