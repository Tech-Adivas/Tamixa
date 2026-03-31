package com.tamixa.application.narration.impl

import com.tamixa.application.narration.TokenUsageService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Distributed daily narration token counter for multi-instance deployments.
 * Active when [app.rate-limit.use-redis] is true (same Redis as rate limiting / bulk jobs).
 */
@Service
@ConditionalOnProperty(name = ["app.rate-limit.use-redis"], havingValue = "true")
@ConditionalOnBean(RedisTemplate::class)
class RedisTokenUsageServiceImpl(
    private val redisTemplate: RedisTemplate<String, String>,
    @Value("\${app.narration.daily-token-limit:100000}") private val dailyLimit: Int
) : TokenUsageService {

    private val log = LoggerFactory.getLogger(javaClass)
    private val zone = ZoneId.systemDefault()

    override fun recordAndCheckLimit(tokens: Int): Boolean {
        if (tokens <= 0) return true
        val key = keyForToday()
        return try {
            val newTotal = redisTemplate.opsForValue().increment(key, tokens.toLong()) ?: tokens.toLong()
            val ttlRemaining = redisTemplate.getExpire(key)
            if (ttlRemaining == -1L || ttlRemaining == -2L) {
                redisTemplate.expire(key, ttlUntilEndOfTomorrow())
            }
            newTotal <= dailyLimit
        } catch (e: Exception) {
            log.warn("Redis token usage increment failed; allowing request: {}", e.message)
            true
        }
    }

    override fun isLimitExceeded(): Boolean {
        val key = keyForToday()
        return try {
            val raw = redisTemplate.opsForValue().get(key)?.toLongOrNull() ?: 0L
            raw >= dailyLimit
        } catch (e: Exception) {
            log.warn("Redis token usage read failed; treating as not exceeded: {}", e.message)
            false
        }
    }

    override fun getDailyUsage(): Int {
        val key = keyForToday()
        return try {
            redisTemplate.opsForValue().get(key)?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        } catch (e: Exception) {
            log.warn("Redis token usage read failed: {}", e.message)
            0
        }
    }

    private fun keyForToday(): String = "narration:daily-tokens:${LocalDate.now(zone)}"

    private fun ttlUntilEndOfTomorrow(): Duration {
        val end = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant()
        val seconds = Duration.between(Instant.now(), end).seconds.coerceAtLeast(3600)
        return Duration.ofSeconds(seconds)
    }
}
