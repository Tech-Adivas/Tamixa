package com.araro.api.config

import com.araro.api.exception.ErrorResponse
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory rate limiter per client key (IP or X-Forwarded-For).
 * For multi-instance deployment use Redis-based rate limiting (e.g. Bucket4j with Redis).
 */
@Component
class RateLimitingFilter(
    @Value("\${app.rate-limit.requests-per-minute:100}") private val requestsPerMinute: Long,
    @Value("\${app.rate-limit.enabled:true}") private val enabled: Boolean,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter(), Ordered {

    private val buckets = ConcurrentHashMap<String, Bucket>()

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE + 1

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (!enabled) {
            filterChain.doFilter(request, response)
            return
        }
        // Admin dashboard makes many parallel requests on load; skip rate limit for /api/v1/admin
        val path = request.requestURI.orEmpty()
        if (path.contains("/api/v1/admin")) {
            filterChain.doFilter(request, response)
            return
        }
        val key = clientKey(request)
        val bucket = buckets.computeIfAbsent(key) { createBucket() }
        if (bucket.tryConsume(1)) {
            response.setHeader("X-RateLimit-Limit", requestsPerMinute.toString())
            filterChain.doFilter(request, response)
        } else {
            val errorBody = ErrorResponse(
                message = "Rate limit exceeded",
                status = HttpStatus.TOO_MANY_REQUESTS.value(),
                traceId = MDC.get(RequestTracingFilter.TRACE_ID_MDC_KEY),
                timestamp = Instant.now().toString()
            )
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.characterEncoding = "UTF-8"
            response.writer.write(objectMapper.writeValueAsString(errorBody))
        }
    }

    private fun clientKey(request: HttpServletRequest): String {
        return request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
            ?: request.remoteAddr
            ?: "unknown"
    }

    private fun createBucket(): Bucket {
        val limit = Bandwidth.classic(requestsPerMinute, Refill.greedy(requestsPerMinute, Duration.ofMinutes(1)))
        return Bucket.builder().addLimit(limit).build()
    }
}
