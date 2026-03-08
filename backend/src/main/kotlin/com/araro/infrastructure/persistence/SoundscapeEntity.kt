package com.araro.infrastructure.persistence

import com.araro.domain.SoundscapeCategory
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "soundscapes")
data class SoundscapeEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 255)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val category: SoundscapeCategory,

    @Column(nullable = false)
    val durationSeconds: Int,

    @Column(nullable = false, length = 500)
    val audioUrl: String,

    @Column(length = 1000)
    val description: String?,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
