package com.tamixa.infrastructure.adapter

import com.tamixa.application.port.ReadingStreakRepositoryPort
import com.tamixa.domain.ReadingStreak
import com.tamixa.infrastructure.persistence.ChildJpaRepository
import com.tamixa.infrastructure.persistence.ReadingStreakEntity
import com.tamixa.infrastructure.persistence.ReadingStreakJpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class ReadingStreakRepositoryAdapter(
    private val jpa: ReadingStreakJpaRepository,
    private val childJpaRepository: ChildJpaRepository
) : ReadingStreakRepositoryPort {

    override fun findByChildId(childId: Long): ReadingStreak? =
        jpa.findByChildId(childId)?.toDomain()

    override fun save(streak: ReadingStreak): ReadingStreak {
        val existing = jpa.findByChildId(streak.childId)
        val child = existing?.child ?: childJpaRepository.getReferenceById(streak.childId)
        return jpa.save(
            ReadingStreakEntity(
                id = existing?.id ?: 0,
                child = child,
                currentStreak = streak.currentStreak,
                longestStreak = streak.longestStreak,
                lastReadAt = streak.lastReadAt,
                updatedAt = Instant.now()
            )
        ).toDomain()
    }
}
