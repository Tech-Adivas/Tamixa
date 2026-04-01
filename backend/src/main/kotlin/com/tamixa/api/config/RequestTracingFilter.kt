package com.tamixa.api.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * Sets trace ID in MDC and echoes correlation headers on the response.
 *
 * Incoming precedence: [CORRELATION_ID_HEADER] (gateway), then [TRACE_ID_HEADER], else a new UUID.
 * Invalid or oversized header values are ignored.
 *
 * Required for this application: registered as the outermost custom filter in [SecurityConfig]
 * (and as a servlet filter via `@Component`) so every API request has a trace id for logs and errors.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestTracingFilter : OncePerRequestFilter() {

    companion object {
        const val TRACE_ID_HEADER = "X-Request-Id"
        const val CORRELATION_ID_HEADER = "X-Correlation-Id"
        const val TRACE_ID_MDC_KEY = "traceId"

        private const val MAX_TRACE_ID_LENGTH = 128
        private val TRACE_ID_PATTERN = Regex("^[a-zA-Z0-9._:/-]+$")

        internal fun sanitizeIncomingTraceId(raw: String?): String? {
            if (raw.isNullOrBlank()) return null
            val trimmed = raw.trim()
            if (trimmed.length > MAX_TRACE_ID_LENGTH) return null
            if (!trimmed.matches(TRACE_ID_PATTERN)) return null
            return trimmed
        }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val traceId =
            sanitizeIncomingTraceId(request.getHeader(CORRELATION_ID_HEADER))
                ?: sanitizeIncomingTraceId(request.getHeader(TRACE_ID_HEADER))
                ?: UUID.randomUUID().toString()
        try {
            MDC.put(TRACE_ID_MDC_KEY, traceId)
            response.setHeader(TRACE_ID_HEADER, traceId)
            response.setHeader(CORRELATION_ID_HEADER, traceId)
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(TRACE_ID_MDC_KEY)
        }
    }
}
