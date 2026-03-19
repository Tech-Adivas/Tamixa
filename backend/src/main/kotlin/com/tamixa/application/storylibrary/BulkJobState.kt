package com.tamixa.application.storylibrary

import java.time.Instant

enum class BulkJobStatus { PENDING, RUNNING, COMPLETED, FAILED }

/**
 * In-memory state for an async bulk story generation job.
 * Used for progress polling and one-at-a-time guard.
 */
data class BulkJobState(
    val jobId: String,
    var status: BulkJobStatus,
    val requestedTotal: Int,
    val publish: Boolean,
    var currentIndex: Int,
    var createdCount: Int,
    var failedCount: Int,
    var created: List<Map<String, Any?>>,
    var failed: List<Map<String, Any?>>,
    var errorMessage: String?,
    var startedAt: Instant?,
    var completedAt: Instant?
)
