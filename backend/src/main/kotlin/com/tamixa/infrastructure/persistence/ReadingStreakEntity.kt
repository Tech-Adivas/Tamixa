package com.tamixa.infrastructure.persistence

import com.tamixa.domain.ReadingStreak
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "reading_streaks")
class ReadingStreakEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    val child: ChildEntity,

    @Column(name = "current_streak", nullable = false)
    val currentStreak: Int = 0,

    @Column(name = "longest_streak", nullable = false)
    val longestStreak: Int = 0,

    @Column(name = "last_read_at")
    val lastReadAt: Instant? = null,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
) {
    fun toDomain() = ReadingStreak(
        id = id,
        childId = child.id,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        lastReadAt = lastReadAt,
        updatedAt = updatedAt
    )
}
