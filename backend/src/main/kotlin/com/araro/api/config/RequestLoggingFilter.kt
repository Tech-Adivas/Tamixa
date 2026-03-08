package com.araro.api.config

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
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
class RequestLoggingFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val traceId = MDC.get(RequestTracingFilter.TRACE_ID_MDC_KEY).orEmpty()
        // Log POST/PUT at INFO so pipeline triggers are visible; GET at DEBUG
        val uri = request.requestURI
        val isPipelineTrigger = request.method in listOf("POST", "PUT") &&
            (uri.contains("republish") || uri.contains("trigger") || uri.contains("regenerate") || uri.contains("retry") || (uri.contains("curated-stories") && "PUT" == request.method))
        if (isPipelineTrigger) {
            log.info("{} {} traceId={}", request.method, uri, traceId)
        } else if (log.isDebugEnabled) {
            log.debug("{} {} traceId={}", request.method, request.requestURI, traceId)
        }
        filterChain.doFilter(request, response)
    }
}
