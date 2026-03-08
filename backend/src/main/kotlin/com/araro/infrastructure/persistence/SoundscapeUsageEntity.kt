package com.araro.infrastructure.persistence

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "soundscapes_usage")
data class SoundscapeUsageEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val parentId: Long,

    @Column
    val storyId: Long?,

    @Column(nullable = false)
    val soundscapeId: Long,

    @Column(nullable = false)
    val usageCount: Int = 1,

    @Column(nullable = false)
    val lastUsedAt: Instant = Instant.now(),

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    val updatedAt: Instant = Instant.now()
)
