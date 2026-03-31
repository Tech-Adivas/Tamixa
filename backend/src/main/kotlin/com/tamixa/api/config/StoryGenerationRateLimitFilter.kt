package com.tamixa.api.config

import com.tamixa.api.ApiVersion
import com.tamixa.api.exception.ErrorResponse
import com.tamixa.application.port.ParentRepositoryPort
import com.tamixa.application.subscription.SubscriptionService
import com.tamixa.domain.SubscriptionPlan
import com.tamixa.infrastructure.config.AppProperties
import com.tamixa.infrastructure.observability.SubscriptionMetrics
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.ObjectProvider
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.UrlPathHelper
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Rate limiting for story generation: stricter for free users, relaxed for paid.
 * When [app.rate-limit.use-redis] is true and Redis is available, counters are shared across instances.
 */
@Component
class StoryGenerationRateLimitFilter(
    private val subscriptionService: SubscriptionService,
    private val parentRepository: ParentRepositoryPort,
    private val subscriptionMetrics: SubscriptionMetrics,
    private val appProperties: AppProperties,
    private val objectMapper: ObjectMapper,
    private val redisTemplate: ObjectProvider<RedisTemplate<String, String>>
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)
    private val pathHelper = UrlPathHelper()
    private val buckets = ConcurrentHashMap<String, Bucket>()
    private val recentHits = ConcurrentHashMap<String, MutableList<Long>>()
    private val SUSPICIOUS_WINDOW_MS = 60_000L
    private val SUSPICIOUS_THRESHOLD = 10

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = pathHelper.getPathWithinApplication(request)
        if (request.method != "POST" || !path.endsWith("/stories/generate") || !path.contains(ApiVersion.V1)) {
            filterChain.doFilter(request, response)
            return
        }
        val config = appProperties.rateLimit
        if (!config.enabled) {
            filterChain.doFilter(request, response)
            return
        }
        val redis = if (config.useRedis) redisTemplate.getIfAvailable() else null
        if (redis != null) {
            if (tryRedisLimit(request, response, filterChain, redis)) return
        } else {
            if (config.useRedis) {
                log.warn("app.rate-limit.use-redis=true but RedisTemplate is unavailable; using in-memory story generation limits")
            }
            inMemoryLimit(request, response, filterChain)
        }
    }

    private fun tryRedisLimit(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
        redis: RedisTemplate<String, String>
    ): Boolean {
        val principal = SecurityContextHolder.getContext().authentication?.name
        val ip = clientIp(request)
        val config = appProperties.rateLimit
        return try {
            val minuteWindow = System.currentTimeMillis() / 60_000L
            val hitKey = "${KEY_PREFIX}hit:${sanitizeKey(ip)}:$minuteWindow"
            val hitCount = redis.opsForValue().increment(hitKey) ?: 1L
            if (hitCount == 1L) {
                redis.expire(hitKey, Duration.ofSeconds(120))
            }
            val suspicious = hitCount >= SUSPICIOUS_THRESHOLD
            if (suspicious) subscriptionMetrics.recordSuspiciousActivity()

            val limitPerHour = when {
                suspicious -> config.storyGenerationSuspiciousPerHour
                isFreeUser(principal) -> config.storyGenerationFreePerHour
                else -> config.storyGenerationPaidPerHour
            }
            val hourWindow = System.currentTimeMillis() / 1000 / 3600L
            val userPart = sanitizeKey((principal ?: "anon").take(120))
            val genKey = "${KEY_PREFIX}q:$userPart:${sanitizeKey(ip)}:$hourWindow"
            val count = redis.opsForValue().increment(genKey) ?: 1L
            if (count == 1L) {
                redis.expire(genKey, Duration.ofSeconds(3900))
            }
            if (count <= limitPerHour) {
                filterChain.doFilter(request, response)
            } else {
                log.warn("Story generation rate limit exceeded (Redis): principal={} limit={}/hr", principal, limitPerHour)
                writeTooManyRequests(response)
            }
            true
        } catch (e: Exception) {
            log.warn("Redis story generation rate limit failed: {}", e.message)
            if (config.redisFailOpen) {
                filterChain.doFilter(request, response)
            } else {
                writeServiceUnavailable(response)
            }
            true
        }
    }

    private fun inMemoryLimit(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val config = appProperties.rateLimit
        val principal = SecurityContextHolder.getContext().authentication?.name
        val ip = clientIp(request)
        val suspicious = isSuspiciousInMemory(ip)
        if (suspicious) subscriptionMetrics.recordSuspiciousActivity()
        val limitPerHour = when {
            suspicious -> config.storyGenerationSuspiciousPerHour
            isFreeUser(principal) -> config.storyGenerationFreePerHour
            else -> config.storyGenerationPaidPerHour
        }
        val key = "story:$principal:$ip"
        val bucket = buckets.computeIfAbsent(key) { createBucket(limitPerHour) }
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response)
        } else {
            log.warn("Story generation rate limit exceeded: principal={} limit={}/hr", principal, limitPerHour)
            writeTooManyRequests(response)
        }
    }

    private fun writeTooManyRequests(response: HttpServletResponse) {
        response.setHeader("Retry-After", "60")
        val errorBody = ErrorResponse(
            message = "Story generation rate limit exceeded. Try again later.",
            status = HttpStatus.TOO_MANY_REQUESTS.value(),
            traceId = MDC.get(RequestTracingFilter.TRACE_ID_MDC_KEY),
            timestamp = Instant.now().toString()
        )
        response.status = HttpStatus.TOO_MANY_REQUESTS.value()
        response.contentType = org.springframework.http.MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        response.writer.write(objectMapper.writeValueAsString(errorBody))
    }

    private fun writeServiceUnavailable(response: HttpServletResponse) {
        response.setHeader("Retry-After", "30")
        val errorBody = ErrorResponse(
            message = "Story generation limit check temporarily unavailable. Please retry shortly.",
            status = HttpStatus.SERVICE_UNAVAILABLE.value(),
            traceId = MDC.get(RequestTracingFilter.TRACE_ID_MDC_KEY),
            timestamp = Instant.now().toString()
        )
        response.status = HttpStatus.SERVICE_UNAVAILABLE.value()
        response.contentType = org.springframework.http.MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        response.writer.write(objectMapper.writeValueAsString(errorBody))
    }

    private fun clientIp(request: HttpServletRequest): String =
        request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
            ?: request.remoteAddr ?: "unknown"

    private fun sanitizeKey(s: String): String = s.replace(":", "_").replace(" ", "_").take(64)

    private fun isFreeUser(email: String?): Boolean {
        if (email.isNullOrBlank()) return true
        val parent = parentRepository.findByEmail(email) ?: return true
        val sub = subscriptionService.getOrCreateSubscription(parent.id)
        return sub.plan == SubscriptionPlan.FREE
    }

    private fun isSuspiciousInMemory(ip: String): Boolean {
        val now = System.currentTimeMillis()
        val list = recentHits.computeIfAbsent(ip) { mutableListOf() }
        synchronized(list) {
            list.removeAll { now - it > SUSPICIOUS_WINDOW_MS }
            list.add(now)
            return list.size >= SUSPICIOUS_THRESHOLD
        }
    }

    private fun createBucket(limitPerHour: Int): Bucket {
        val limit = Bandwidth.classic(limitPerHour.toLong(), Refill.greedy(limitPerHour.toLong(), Duration.ofHours(1)))
        return Bucket.builder().addLimit(limit).build()
    }

    private companion object {
        private const val KEY_PREFIX = "sgen:"
    }
}
