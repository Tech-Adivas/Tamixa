package com.tamixa.api.education.dto

import com.tamixa.domain.ReadingLevel
import com.tamixa.domain.ReadingLevelAssessment
import java.time.Instant

data class ReadingLevelResponse(
    val childId: Long,
    val level: Int,
    val updatedAt: Instant
) {
    companion object {
        fun from(domain: ReadingLevel) = ReadingLevelResponse(
            childId = domain.childId,
            level = domain.level,
            updatedAt = domain.updatedAt
        )
    }
}

data class ReadingLevelAssessmentResponse(
    val childId: Long,
    val oldLevel: Int,
    val newLevel: Int,
    val assessedAt: Instant
) {
    companion object {
        fun from(domain: ReadingLevelAssessment) = ReadingLevelAssessmentResponse(
            childId = domain.childId,
            oldLevel = domain.oldLevel,
            newLevel = domain.newLevel,
            assessedAt = domain.assessedAt
        )
    }
}

data class ReadingLevelHistoryResponse(
    val assessments: List<ReadingLevelAssessmentResponse>
)
