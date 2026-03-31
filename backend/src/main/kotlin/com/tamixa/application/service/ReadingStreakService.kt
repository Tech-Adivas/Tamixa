package com.tamixa.application.service

import com.tamixa.application.port.ReadingStreakRepositoryPort
import com.tamixa.domain.ReadingStreak
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class ReadingStreakService(
    private val readingStreakRepository: ReadingStreakRepositoryPort
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getOrCreateStreak(childId: Long): ReadingStreak {
        return readingStreakRepository.findByChildId(childId)
            ?: run {
                log.debug("Creating default reading streak for child={}", childId)
                readingStreakRepository.save(
                    ReadingStreak(
                        id = 0,
                        childId = childId,
                        currentStreak = 0,
                        longestStreak = 0,
                        lastReadAt = null,
                        updatedAt = Instant.now()
                    )
                )
            }
    }

    fun recordStoryRead(childId: Long): ReadingStreak {
        val current = getOrCreateStreak(childId)
        val now = Instant.now()

        val newStreak = if (current.lastReadAt == null) {
            1
        } else {
            val daysSinceLastRead = ChronoUnit.DAYS.between(current.lastReadAt, now)
            when {
                daysSinceLastRead == 1L -> current.currentStreak + 1
                daysSinceLastRead > 1L -> 1
                else -> current.currentStreak
            }
        }

        val updated = current.copy(
            currentStreak = newStreak,
            longestStreak = maxOf(newStreak, current.longestStreak),
            lastReadAt = now,
            updatedAt = now
        )

        log.debug("Streak updated for child={}: current={} longest={}", childId, newStreak, updated.longestStreak)
        return readingStreakRepository.save(updated)
    }

    fun getStreakStats(childId: Long): Map<String, Any> {
        val streak = getOrCreateStreak(childId)
        return mapOf(
            "currentStreak" to streak.currentStreak,
            "longestStreak" to streak.longestStreak,
            "lastReadAt" to (streak.lastReadAt?.toString() ?: "never"),
            "isActive" to streak.isStreakActive(Instant.now())
        )
    }
}
