package com.tamixa.domain

import java.time.Instant

data class ReadingStreak(
    val id: Long,
    val childId: Long,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastReadAt: Instant?,
    val updatedAt: Instant
) {
    fun isStreakActive(now: Instant): Boolean {
        if (lastReadAt == null) return false
        val daysSinceLastRead = java.time.temporal.ChronoUnit.DAYS.between(lastReadAt, now)
        return daysSinceLastRead <= 1
    }
}
