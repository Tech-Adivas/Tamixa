package com.araro.infrastructure.observability

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Logs stream access for audit and security.
 * In production, emit to structured log (JSON) for SIEM/analytics.
 */
@Component
class StreamAccessLogger {

    private val log = LoggerFactory.getLogger(javaClass)

    fun logStreamAccess(
        storyId: Long,
        language: String,
        parentId: Long?,
        path: String
    ) {
        log.info(
            "STREAM_ACCESS storyId={} language={} parentId={} path={}",
            storyId, language, parentId, path
        )
    }
}
