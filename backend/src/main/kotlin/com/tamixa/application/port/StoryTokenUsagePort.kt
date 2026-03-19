package com.tamixa.application.port

/**
 * Persists token usage per story for cost tracking and observability.
 */
interface StoryTokenUsagePort {

    fun recordUsage(storyId: Long, promptTokens: Int, completionTokens: Int, totalTokens: Int)
}
