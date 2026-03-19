package com.tamixa.infrastructure.narration

import com.tamixa.domain.TranslationPipelineStatus
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * Non-blocking status updates for the narration pipeline.
 * Uses JdbcTemplate (no JPA) to avoid entity-manager lock contention when updating
 * story_translations.status from within a long-running pipeline transaction.
 *
 * Uses NOT_SUPPORTED (not REQUIRES_NEW) to avoid connection pool exhaustion: when 6
 * pipeline threads all call updateStatus, REQUIRES_NEW would need 12 connections
 * (6 suspended + 6 new). NOT_SUPPORTED suspends current tx and runs without one—1 conn per call.
 */
@Component
class PipelineStatusUpdater(
    private val jdbcTemplate: JdbcTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun updateStatus(translationId: Long, status: TranslationPipelineStatus, lastError: String?): Boolean {
        return try {
            val rows = jdbcTemplate.update(
                "UPDATE story_translations SET status = ?, last_error = ? WHERE id = ?",
                status.name,
                lastError?.take(500),
                translationId
            )
            rows > 0
        } catch (e: Exception) {
            log.warn("Pipeline status update failed translationId={} status={}: {}", translationId, status, e.message)
            false
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun incrementRetryCount(translationId: Long, lastError: String): Boolean {
        return try {
            val rows = jdbcTemplate.update(
                "UPDATE story_translations SET retry_count = retry_count + 1, last_error = ? WHERE id = ?",
                lastError.take(500),
                translationId
            )
            rows > 0
        } catch (e: Exception) {
            log.warn("Retry count increment failed translationId={}: {}", translationId, e.message)
            false
        }
    }
}
