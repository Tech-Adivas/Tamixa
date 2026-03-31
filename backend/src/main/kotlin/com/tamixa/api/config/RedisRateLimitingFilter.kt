package com.tamixa.api.config

import com.tamixa.api.exception.ErrorResponse
import com.tamixa.infrastructure.config.AppProperties
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Instant

/**
 * Redis-backed rate limiter. Use for multi-instance deployments.
 * Fixed-window counter: key = rl:{auth|session|admin|general}:{clientKey}:{minute}, INCR, EXPIRE 60.
 * When RATE_LIMIT_USE_REDIS=true and Redis is available, this filter runs instead of RateLimitingFilter.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@ConditionalOnProperty(name = ["app.rate-limit.use-redis"], havingValue = "true")
@ConditionalOnBean(RedisTemplate::class)
class RedisRateLimitingFilter(
    private val appProperties: AppProperties,
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

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
        val bucketPrefix = when {
            STRICT_AUTH_PATHS.any { path.contains(it) } -> "auth"
            SESSION_PATHS.any { path.contains(it) } -> "session"
            path.contains("/api/v1/admin") -> "admin"
            else -> "general"
        }
        val limit = when (bucketPrefix) {
            "auth" -> config.authRequestsPerMinute
            "session" -> config.sessionRequestsPerMinute
            "admin" -> config.adminRequestsPerMinute
            else -> config.requestsPerMinute
        }
        val key = clientKey(request)
        val window = System.currentTimeMillis() / 1000 / WINDOW_SECONDS
        val redisKey = "$KEY_PREFIX$bucketPrefix:$key:$window"

        try {
            val count = redisTemplate.opsForValue().increment(redisKey) ?: 1L
            if (count == 1L) {
                redisTemplate.expire(redisKey, java.time.Duration.ofSeconds(WINDOW_SECONDS))
            }
            if (count <= limit) {
                response.setHeader("X-RateLimit-Limit", limit.toString())
                response.setHeader("X-RateLimit-Remaining", (limit - count).coerceAtLeast(0).toString())
                filterChain.doFilter(request, response)
            } else {
                respondRateLimited(response)
            }
        } catch (e: Exception) {
            val log = org.slf4j.LoggerFactory.getLogger(javaClass)
            log.warn("Redis rate limit check failed: {}", e.message)
            if (config.redisFailOpen) {
                filterChain.doFilter(request, response)
            } else {
                respondRateLimitUnavailable(response)
            }
        }
    }

    private fun clientKey(request: HttpServletRequest): String =
        request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
            ?: request.remoteAddr
            ?: "unknown"

    private fun respondRateLimited(response: HttpServletResponse) {
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

    private fun respondRateLimitUnavailable(response: HttpServletResponse) {
        response.setHeader("Retry-After", "30")
        val errorBody = ErrorResponse(
            message = "Rate limit check temporarily unavailable. Please retry shortly.",
            status = HttpStatus.SERVICE_UNAVAILABLE.value(),
            traceId = MDC.get(RequestTracingFilter.TRACE_ID_MDC_KEY),
            timestamp = Instant.now().toString()
        )
        response.status = HttpStatus.SERVICE_UNAVAILABLE.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        response.writer.write(objectMapper.writeValueAsString(errorBody))
    }

    private companion object {
        private const val KEY_PREFIX = "rl:"
        private const val WINDOW_SECONDS = 60L
        private val STRICT_AUTH_PATHS = listOf(
            "/auth/register", "/auth/login", "/auth/passwordless",
            "/auth/otp/send", "/auth/otp/verify"
        )
        private val SESSION_PATHS = listOf("/api/v1/auth/me", "/api/v1/auth/refresh")
    }
}
