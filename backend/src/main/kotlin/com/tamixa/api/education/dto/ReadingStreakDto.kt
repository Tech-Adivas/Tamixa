package com.tamixa.api.education.dto

import com.tamixa.domain.ReadingStreak
import java.time.Instant

data class ReadingStreakResponse(
    val childId: Long,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastReadAt: Instant?,
    val isActive: Boolean
) {
    companion object {
        fun from(domain: ReadingStreak) = ReadingStreakResponse(
            childId = domain.childId,
            currentStreak = domain.currentStreak,
            longestStreak = domain.longestStreak,
            lastReadAt = domain.lastReadAt,
            isActive = domain.isStreakActive(Instant.now())
        )
    }
}

data class StreakStatsResponse(
    val currentStreak: Int,
    val longestStreak: Int,
    val lastReadAt: String,
    val isActive: Boolean
)
