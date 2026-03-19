package com.tamixa.api.config

import com.tamixa.api.exception.ErrorResponse
import com.tamixa.infrastructure.config.AppProperties
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
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
 * Admin paths use a higher limit (no full bypass) to prevent abuse if admin JWT is compromised.
 * When app.rate-limit.use-redis=true, RedisRateLimitingFilter is used instead.
 */
@Component
@ConditionalOnProperty(name = ["app.rate-limit.use-redis"], havingValue = "false", matchIfMissing = true)
class RateLimitingFilter(
    private val appProperties: AppProperties,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter(), Ordered {

    private val buckets = ConcurrentHashMap<String, Bucket>()
    private val adminBuckets = ConcurrentHashMap<String, Bucket>()

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE + 1

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val config = appProperties.rateLimit
        if (!config.enabled) {
            filterChain.doFilter(request, response)
            return
        }
        val path = request.requestURI.orEmpty()
        val isAdminPath = path.contains("/api/v1/admin")
        val limit = if (isAdminPath) config.adminRequestsPerMinute else config.requestsPerMinute
        val bucketMap = if (isAdminPath) adminBuckets else buckets
        val key = clientKey(request)
        val bucket = bucketMap.computeIfAbsent(key) { createBucket(limit) }
        if (bucket.tryConsume(1)) {
            response.setHeader("X-RateLimit-Limit", limit.toString())
            response.setHeader("X-RateLimit-Remaining", bucket.getAvailableTokens().coerceAtLeast(0).toString())
            filterChain.doFilter(request, response)
        } else {
            response.setHeader("Retry-After", "60")
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

    private fun createBucket(requestsPerMinute: Long): Bucket {
        val limit = Bandwidth.classic(requestsPerMinute, Refill.greedy(requestsPerMinute, Duration.ofMinutes(1)))
        return Bucket.builder().addLimit(limit).build()
    }
}
