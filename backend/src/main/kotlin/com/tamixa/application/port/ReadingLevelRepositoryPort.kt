package com.tamixa.application.port

import com.tamixa.domain.ReadingLevel
import com.tamixa.domain.ReadingLevelAssessment

interface ReadingLevelRepositoryPort {
    fun findByChildId(childId: Long): ReadingLevel?
    fun save(readingLevel: ReadingLevel): ReadingLevel
    fun saveAssessment(assessment: ReadingLevelAssessment): ReadingLevelAssessment
    fun findAssessmentsByChildId(childId: Long): List<ReadingLevelAssessment>
}
