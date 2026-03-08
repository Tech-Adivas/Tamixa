package com.araro.infrastructure.http

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.web.client.HttpStatusCodeException

/**
 * Extracts clear error messages from API exception responses for frontend display.
 * Supports OpenAI/Google format: {"error": {"message": "..."}}
 */
object ApiErrorExtractor {

    fun extract(e: Exception, objectMapper: ObjectMapper, maxLength: Int = 300): String {
        val msg = e.message ?: "Unknown error"
        if (e is HttpStatusCodeException) {
            val extracted = extractFromResponseBody(e, objectMapper)
            if (extracted != null && extracted.isNotBlank()) return extracted.take(maxLength)
        }
        // Check cause chain for wrapped HTTP errors
        var cause: Throwable? = e.cause
        while (cause != null) {
            if (cause is HttpStatusCodeException) {
                val extracted = extractFromResponseBody(cause, objectMapper)
                if (extracted != null && extracted.isNotBlank()) return extracted.take(maxLength)
            }
            cause = cause.cause
        }
        return msg.take(maxLength)
    }

    private fun extractFromResponseBody(e: HttpStatusCodeException, objectMapper: ObjectMapper): String? {
        val body = try { e.responseBodyAsString } catch (_: Exception) { null } ?: return null
        val json = try { objectMapper.readTree(body) } catch (_: Exception) { return null }
        val errNode = json.path("error").path("message")
        return if (!errNode.isMissingNode && errNode.isTextual) {
            errNode.asText().trim().takeIf { it.isNotBlank() }
        } else null
    }
}
