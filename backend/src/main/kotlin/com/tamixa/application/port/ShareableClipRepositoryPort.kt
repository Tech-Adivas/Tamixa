package com.tamixa.application.port

import java.time.Instant

/**
 * Repository for shareable clips (Premium+ feature).
 */
interface ShareableClipRepositoryPort {

    fun save(clip: ShareableClip): ShareableClip

    fun findById(id: Long): ShareableClip?

    fun findByIdAndParentId(id: Long, parentId: Long): ShareableClip?

    fun countByParentIdSince(parentId: Long, since: Instant): Int

    fun updateStatusAndStorage(id: Long, status: String, storagePath: String?, errorMessage: String?)

    /** Mark download analytics as emitted (idempotent). Returns true if was previously unset. */
    fun markDownloadAnalyticsEmitted(id: Long): Boolean
}

data class ShareableClip(
    val id: Long,
    val parentId: Long,
    val storyId: Long,
    val storySource: String,
    val language: String,
    val voiceProfile: String,
    val startSeconds: Int,
    val durationSeconds: Int,
    val format: String,
    val storagePath: String?,
    val status: String,
    val processingJobId: Long?,
    val errorMessage: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val downloadAnalyticsEmittedAt: Instant? = null
)
