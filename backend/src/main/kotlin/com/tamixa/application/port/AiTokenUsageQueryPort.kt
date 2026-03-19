package com.tamixa.application.port

import java.time.Instant

/**
 * Queries AI token usage for cost protection and observability.
 * Used by TokenLimitGuard for per-parent daily and system-wide limits.
 */
interface AiTokenUsageQueryPort {

    /** Sum of total tokens used by [parentId] between [start] and [end]. */
    fun getTokensUsedByParent(parentId: Long, start: Instant, end: Instant): Long

    /** Sum of total tokens used system-wide between [start] and [end]. */
    fun getTokensUsedSystemWide(start: Instant, end: Instant): Long
}
