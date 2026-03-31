package com.tamixa.application.storylibrary

import java.time.Instant

enum class BulkJobStatus { PENDING, RUNNING, COMPLETED, FAILED }

/**
 * State for an async bulk story generation job.
 * Used for progress polling and one-at-a-time guard.
 * Immutable for Redis serialization; use copy() to update.
 */
data class BulkJobState(
    val jobId: String,
    val status: BulkJobStatus,
    val requestedTotal: Int,
    val publish: Boolean,
    val currentIndex: Int,
    val createdCount: Int,
    val failedCount: Int,
    val created: List<Map<String, Any?>>,
    val failed: List<Map<String, Any?>>,
    val errorMessage: String?,
    val startedAt: Instant?,
    val completedAt: Instant?
)
