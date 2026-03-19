package com.tamixa.api.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Logs incoming HTTP requests at DEBUG level for traceability.
 * Runs after RequestTracingFilter so traceId is available in MDC.
 * Skips DEBUG for high-frequency polling endpoints (pipeline/active-now, stories/with-issues).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
class RequestLoggingFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    /** Paths that poll every few seconds; skip DEBUG to reduce log noise. */
    private val silentPaths = setOf(
        "/api/v1/admin/pipeline/active-now",
        "/api/v1/admin/stories/with-issues"
    )

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val traceId = MDC.get(RequestTracingFilter.TRACE_ID_MDC_KEY).orEmpty()
        val uri = request.requestURI
        val isPipelineTrigger = request.method in listOf("POST", "PUT") &&
            (uri.contains("republish") || uri.contains("trigger") || uri.contains("regenerate") || uri.contains("retry") || (uri.contains("/stories/") && "PUT" == request.method) || uri.contains("clear-all-audio-and-cache"))
        val isSilentPolling = uri in silentPaths
        if (isPipelineTrigger) {
            log.info("{} {} traceId={} (start)", request.method, uri, traceId)
        } else if (!isSilentPolling && log.isDebugEnabled) {
            log.debug("{} {} traceId={}", request.method, uri, traceId)
        }
        filterChain.doFilter(request, response)
        if (isPipelineTrigger) {
            log.info("{} {} -> {} traceId={}", request.method, uri, response.status, traceId)
        }
    }
}
