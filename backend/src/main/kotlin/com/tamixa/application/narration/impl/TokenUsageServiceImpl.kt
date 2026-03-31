package com.tamixa.application.narration.impl

import com.tamixa.application.narration.TokenUsageService
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tracks daily token usage in memory. Resets at midnight.
 * For production scale, consider Redis with TTL.
 */
@Service
@ConditionalOnProperty(name = ["app.rate-limit.use-redis"], havingValue = "false", matchIfMissing = true)
class TokenUsageServiceImpl(
    @Value("\${app.narration.daily-token-limit:100000}") private val dailyLimit: Int
) : TokenUsageService {

    private val usage = AtomicInteger(0)
    private var lastResetDate: LocalDate = LocalDate.now()

    override fun recordAndCheckLimit(tokens: Int): Boolean {
        resetIfNewDay()
        val newTotal = usage.addAndGet(tokens)
        return newTotal <= dailyLimit
    }

    override fun isLimitExceeded(): Boolean {
        resetIfNewDay()
        return usage.get() >= dailyLimit
    }

    override fun getDailyUsage(): Int {
        resetIfNewDay()
        return usage.get()
    }

    private fun resetIfNewDay() {
        val today = LocalDate.now()
        if (today != lastResetDate) {
            usage.set(0)
            lastResetDate = today
        }
    }
}
