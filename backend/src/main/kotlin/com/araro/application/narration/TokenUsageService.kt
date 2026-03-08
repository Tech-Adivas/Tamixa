package com.araro.application.narration

/**
 * Tracks daily token usage for narration formatting.
 * Enforces limit to control cost; fallback to simple SSML when exceeded.
 */
interface TokenUsageService {

    /**
     * Record token usage and check if daily limit exceeded.
     * @param tokens Tokens used (prompt + completion)
     * @return true if within limit, false if limit exceeded (caller should skip AI formatting)
     */
    fun recordAndCheckLimit(tokens: Int): Boolean

    /**
     * Check if daily limit is already exceeded without recording.
     */
    fun isLimitExceeded(): Boolean

    /**
     * Get current day's usage for observability.
     */
    fun getDailyUsage(): Int
}
