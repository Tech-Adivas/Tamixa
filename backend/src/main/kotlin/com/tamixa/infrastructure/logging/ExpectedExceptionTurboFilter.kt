package com.tamixa.infrastructure.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.core.spi.FilterReply
import com.tamixa.application.story.ContentModerationException
import com.tamixa.application.storylibrary.ContentUnchangedException
import com.tamixa.application.storylibrary.PipelineRunningException

/**
 * Demotes expected business exceptions from ERROR to DEBUG.
 * These are handled by @ExceptionHandler and return 400/409; the full stack trace in logs is noise.
 */
class ExpectedExceptionTurboFilter : ch.qos.logback.classic.turbo.TurboFilter() {

    override fun decide(
        marker: org.slf4j.Marker?,
        logger: ch.qos.logback.classic.Logger,
        level: Level,
        format: String?,
        params: Array<out Any?>?,
        t: Throwable?
    ): FilterReply {
        if (level.toInt() >= Level.WARN.toInt() && isExpectedException(t)) {
            return FilterReply.DENY
        }
        return FilterReply.NEUTRAL
    }

    private fun isExpectedException(t: Throwable?): Boolean {
        var current: Throwable? = t
        while (current != null) {
            when (current) {
                is ContentModerationException,
                is ContentUnchangedException,
                is PipelineRunningException -> return true
            }
            current = current.cause
        }
        return false
    }
}
