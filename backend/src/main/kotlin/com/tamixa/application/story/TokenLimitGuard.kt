package com.tamixa.application.story

import com.tamixa.application.port.AiTokenUsageQueryPort
import com.tamixa.infrastructure.config.AppProperties
import com.tamixa.infrastructure.observability.ApplicationMetrics
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * Cost protection: enforces token limits before story generation.
 * - Per-parent daily AI token limit
 * - System-wide token limit
 * - Circuit breaker if token usage spikes (rejects when system daily exceeds spike threshold)
 *
 * Call [checkBeforeGeneration] before invoking OpenAI. Throws if limits exceeded.
 */
@Component
class TokenLimitGuard(
    private val tokenUsageQuery: AiTokenUsageQueryPort,
    private val appProperties: AppProperties,
    private val metrics: ApplicationMetrics
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private fun startOfTodayUtc(): Instant =
        Instant.now().atZone(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS).toInstant()

    private fun endOfTodayUtc(): Instant = startOfTodayUtc().plus(1, ChronoUnit.DAYS)

    /**
     * Checks token limits before allowing story generation.
     * Updates ai_token_daily_usage metric.
     * Throws [TokenLimitExceededException] if any limit exceeded.
     */
    fun checkBeforeGeneration(parentId: Long, estimatedTokens: Int = 1024) {
        val config = appProperties.aiTokenLimits
        val start = startOfTodayUtc()
        val end = endOfTodayUtc()

        val parentUsed = tokenUsageQuery.getTokensUsedByParent(parentId, start, end)
        val systemUsed = tokenUsageQuery.getTokensUsedSystemWide(start, end)

        // Update observability gauge
        metrics.setAiTokenDailyUsage(systemUsed)

        // Circuit breaker: if system spike detected, block all new requests
        if (systemUsed >= config.circuitBreakerSpikeThreshold) {
            log.warn("Token circuit breaker triggered: system daily usage {} >= threshold {}",
                systemUsed, config.circuitBreakerSpikeThreshold)
            throw TokenLimitExceededException("AI service temporarily unavailable. Try again later.")
        }

        // Per-parent daily limit
        if (parentUsed + estimatedTokens > config.perParentDaily) {
            log.warn("Parent {} exceeded daily token limit: {} + {} > {}",
                parentId, parentUsed, estimatedTokens, config.perParentDaily)
            throw TokenLimitExceededException("Daily story generation limit reached. Try again tomorrow.")
        }

        // System-wide daily limit
        if (systemUsed + estimatedTokens > config.systemWideDaily) {
            log.warn("System-wide token limit exceeded: {} + {} > {}",
                systemUsed, estimatedTokens, config.systemWideDaily)
            throw TokenLimitExceededException("AI service temporarily unavailable. Try again later.")
        }
    }
}

/** Thrown when token limits are exceeded (per-parent or system-wide). */
class TokenLimitExceededException(message: String) : RuntimeException(message)
