package com.tamixa.api.exception

import java.time.Instant

/**
 * Structured JSON error body for filter/security error responses.
 * Used by rate limiting, auth entry point, and access denied handler.
 */
data class ErrorResponse(
    val message: String,
    val status: Int,
    val traceId: String?,
    val timestamp: String
) {
    /** Builds a map with the same shape for use in @RestControllerAdvice (message, status, traceId, timestamp, optional errors). */
    companion object {
        fun body(
            message: String,
            status: Int,
            traceId: String? = null,
            errors: Map<String, String>? = null,
            code: String? = null,
        ): Map<String, Any> {
            val map = mutableMapOf<String, Any>(
                "message" to message,
                "status" to status,
                "traceId" to (traceId ?: ""),
                "timestamp" to Instant.now().toString()
            )
            if (!errors.isNullOrEmpty()) map["errors"] = errors
            if (!code.isNullOrBlank()) map["code"] = code
            return map
        }
    }
}
