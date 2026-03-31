package com.tamixa.domain

import java.time.Instant

data class ReadingLevel(
    val id: Long,
    val childId: Long,
    val level: Int,
    val updatedAt: Instant
) {
    companion object {
        const val MIN_LEVEL = 1
        const val MAX_LEVEL = 10
        const val DEFAULT_LEVEL = 1
    }
}

data class ReadingLevelAssessment(
    val id: Long,
    val childId: Long,
    val quizId: Long?,
    val oldLevel: Int,
    val newLevel: Int,
    val assessedAt: Instant
)
