package com.tamixa.api.config

import com.tamixa.api.ApiVersion
import com.tamixa.api.exception.ErrorResponse
import com.tamixa.application.port.ParentRepositoryPort
import com.tamixa.application.subscription.SubscriptionService
import com.tamixa.infrastructure.config.AppProperties
import com.tamixa.infrastructure.observability.SubscriptionMetrics
import com.tamixa.domain.SubscriptionPlan
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.http.HttpStatus
import org.springframework.web.util.UrlPathHelper
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Rate limiting for story generation: stricter for free users, relaxed for paid.
 * Fraud prevention: mitigates abuse; suspicious activity gets minimal quota.
 */
@Component
class StoryGenerationRateLimitFilter(
    private val subscriptionService: SubscriptionService,
    private val parentRepository: ParentRepositoryPort,
    private val subscriptionMetrics: SubscriptionMetrics,
    private val appProperties: AppProperties,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)
    private val pathHelper = UrlPathHelper()
    private val buckets = ConcurrentHashMap<String, Bucket>()
    private val recentHits = ConcurrentHashMap<String, MutableList<Long>>()
    private val SUSPICIOUS_WINDOW_MS = 60_000L  // 1 min
    private val SUSPICIOUS_THRESHOLD = 10  // 10 req/min from same IP = suspicious

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
        val principal = SecurityContextHolder.getContext().authentication?.name
        val ip = request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
            ?: request.remoteAddr ?: "unknown"
        val suspicious = isSuspicious(ip)
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
    }

    private fun isFreeUser(email: String?): Boolean {
        if (email.isNullOrBlank()) return true
        val parent = parentRepository.findByEmail(email) ?: return true
        val sub = subscriptionService.getOrCreateSubscription(parent.id)
        return sub.plan == SubscriptionPlan.FREE
    }

    private fun isSuspicious(ip: String): Boolean {
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
}
