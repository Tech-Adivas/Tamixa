package com.tamixa.infrastructure.observability

import com.tamixa.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks which language is currently being processed per story.
 * Used by pipeline-status API to show "Generating X audio..." on frontend.
 * Stale entries (e.g. from crash) are auto-cleared after totalTimeout + buffer to prevent "zombie" active state.
 */
@Component
class PipelineProgressTracker(
    private val appProperties: AppProperties
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private data class Entry(val language: String, val startedAt: Long = System.currentTimeMillis())

    private val activeEntries = ConcurrentHashMap<Long, Entry>()

    private fun staleThresholdMs(): Long {
        val totalMin = appProperties.translationPipeline.totalTimeoutMinutes.coerceIn(10, 120)
        return (totalMin + 15).toLong() * 60 * 1000L  // totalTimeout + 15 min buffer
    }

    fun setProcessing(masterStoryId: Long, language: String) {
        activeEntries[masterStoryId] = Entry(language)
    }

    /** Claim pipeline slot for story. Returns true if claimed, false if another pipeline is already running.
     * If the existing claim is older than claimMaxAgeMinutes, it is cleared and the new run is allowed (avoids indefinite block when stuck). */
    fun tryClaimForPipeline(masterStoryId: Long): Boolean {
        removeStaleEntries()
        val maxAge = appProperties.translationPipeline.claimMaxAgeMinutes.coerceIn(5, 60)
        if (clearStuckIfOlderThan(masterStoryId, maxAge)) {
            log.info("Pipeline claim cleared for masterStoryId={} (age > {}min); allowing new run", masterStoryId, maxAge)
        }
        return activeEntries.putIfAbsent(masterStoryId, Entry("starting", System.currentTimeMillis())) == null
    }

    fun clearProcessing(masterStoryId: Long) {
        activeEntries.remove(masterStoryId)
    }

    /** Clear stuck entry if older than maxAgeMinutes. Returns true if cleared, false if not present or too fresh. */
    fun clearStuckIfOlderThan(masterStoryId: Long, maxAgeMinutes: Int): Boolean {
        val entry = activeEntries[masterStoryId] ?: return false
        val ageMs = System.currentTimeMillis() - entry.startedAt
        if (ageMs < maxAgeMinutes * 60L * 1000L) return false
        activeEntries.remove(masterStoryId)
        return true
    }

    /** Minutes since this story started processing, or null if not tracked. */
    fun getProcessingAgeMinutes(masterStoryId: Long): Int? {
        val entry = activeEntries[masterStoryId] ?: return null
        return ((System.currentTimeMillis() - entry.startedAt) / (60 * 1000)).toInt()
    }

    fun getProcessingLanguage(masterStoryId: Long): String? {
        removeStaleEntries()
        return activeEntries[masterStoryId]?.language
    }

    /** True if any story pipeline is currently running. Cleans stale entries before checking. */
    fun hasAnyProcessing(): Boolean {
        removeStaleEntries()
        return activeEntries.isNotEmpty()
    }

    /** Active stories: storyId to language. For banner/UI to show "Generating X for story #Y". */
    fun getActiveStories(): Map<Long, String> {
        removeStaleEntries()
        return activeEntries.mapValues { it.value.language }
    }

    private fun removeStaleEntries() {
        val now = System.currentTimeMillis()
        val threshold = staleThresholdMs()
        val toRemove = activeEntries.filter { now - it.value.startedAt > threshold }.keys.toList()
        toRemove.forEach { activeEntries.remove(it) }
    }
}
