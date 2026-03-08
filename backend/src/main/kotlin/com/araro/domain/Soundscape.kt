package com.araro.domain

import java.time.Instant

/**
 * Background soundscape for story mood enhancement.
 * Categories: CALM, ADVENTUROUS, FANTASY, BEDTIME, NATURE
 */
data class Soundscape(
    val id: Long,
    val name: String,
    val category: SoundscapeCategory,
    val durationSeconds: Int,
    val audioUrl: String,
    val description: String?,
    val createdAt: Instant
)

enum class SoundscapeCategory {
    CALM,
    ADVENTUROUS,
    FANTASY,
    BEDTIME,
    NATURE
}
