package com.tamixa.ui.edu

import kotlin.math.roundToInt

/**
 * Presents an interactive story as one story across its separate scene clips: whole-story progress, an estimated
 * whole-story length, and where the questions sit on the progress bar.
 *
 * A scene's share of the story is its word count when every scene has text, otherwise all scenes count the same.
 * What is still ahead is the longest way from the current scene to an ending, so progress never moves backwards
 * after a choice: a shorter branch only makes it jump ahead.
 */
class InteractiveStoryTimeline(private val graph: InteractiveStoryGraph) {

    private val useWordCounts = graph.segments.values.all { wordCount(it.text) > 0 }
    private val remainingCache = HashMap<String, Float>()

    /** Share of the story [segmentId] takes (0 for an unknown scene). */
    fun weight(segmentId: String): Float {
        val segment = graph.segments[segmentId] ?: return 0f
        return if (useWordCounts) wordCount(segment.text).toFloat() else 1f
    }

    /** Longest rest of the story after [segmentId], not counting the scene itself. Loops in the graph are cut. */
    fun remainingAfter(segmentId: String): Float =
        remainingCache.getOrPut(segmentId) { longestAfter(segmentId, setOf(segmentId)) }

    private fun longestAfter(segmentId: String, onPath: Set<String>): Float =
        graph.segments[segmentId]?.choices.orEmpty()
            .map { it.nextSegmentId }
            .filter { it in graph.segments && it !in onPath }
            .maxOfOrNull { next -> weight(next) + longestAfter(next, onPath + next) }
            ?: 0f

    /** Whole-story progress 0..1: the [played] scenes are done and [current] is at [clipProgress] (0..1). */
    fun progress(played: List<String>, current: String, clipProgress: Float): Float {
        val done = played.sumOf { weight(it).toDouble() }.toFloat()
        val currentWeight = weight(current)
        val total = done + currentWeight + remainingAfter(current)
        if (total <= 0f) return clipProgress.coerceIn(0f, 1f)
        return ((done + currentWeight * clipProgress.coerceIn(0f, 1f)) / total).coerceIn(0f, 1f)
    }

    /**
     * Estimated whole-story length in seconds, scaled up from what is known: [playedSeconds] for the played scenes
     * and [currentClipSeconds] for the current clip. Null while the current clip's length isn't known yet.
     */
    fun durationSeconds(played: List<String>, current: String, playedSeconds: Int, currentClipSeconds: Int): Int? {
        if (currentClipSeconds <= 0) return null
        val known = played.sumOf { weight(it).toDouble() }.toFloat() + weight(current)
        if (known <= 0f) return null
        val total = known + remainingAfter(current)
        return ((playedSeconds + currentClipSeconds) * total / known).roundToInt().coerceAtLeast(1)
    }

    /**
     * Where questions sit on the whole-story bar (0..1): after each played scene (each one ended with a question)
     * and after [current] when it asks one.
     */
    fun questionMarks(played: List<String>, current: String): List<Float> {
        val total = played.sumOf { weight(it).toDouble() }.toFloat() + weight(current) + remainingAfter(current)
        if (total <= 0f) return emptyList()
        val ends = mutableListOf<Float>()
        var sum = 0f
        played.forEach { sum += weight(it); ends += sum / total }
        if (graph.segments[current]?.choices.orEmpty().isNotEmpty()) ends += (sum + weight(current)) / total
        return ends.filter { it in 0.01f..0.99f }
    }

    private fun wordCount(text: String): Int = text.split(Regex("\\s+")).count { it.isNotBlank() }
}
