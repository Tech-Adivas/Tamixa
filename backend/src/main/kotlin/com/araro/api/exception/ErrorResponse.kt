package com.araro.api.exception

/**
 * Structured JSON error body for filter/security error responses.
 * Used by rate limiting, auth entry point, and access denied handler.
 */
data class ErrorResponse(
    val message: String,
    val status: Int,
    val traceId: String?,
    val timestamp: String
)
