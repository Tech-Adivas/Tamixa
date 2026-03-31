package com.tamixa.infrastructure.adapter

import com.tamixa.application.port.ReadingLevelRepositoryPort
import com.tamixa.domain.ReadingLevel
import com.tamixa.domain.ReadingLevelAssessment
import com.tamixa.infrastructure.persistence.ChildJpaRepository
import com.tamixa.infrastructure.persistence.ReadingLevelAssessmentEntity
import com.tamixa.infrastructure.persistence.ReadingLevelAssessmentJpaRepository
import com.tamixa.infrastructure.persistence.ReadingLevelEntity
import com.tamixa.infrastructure.persistence.ReadingLevelJpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class ReadingLevelRepositoryAdapter(
    private val readingLevelJpaRepository: ReadingLevelJpaRepository,
    private val assessmentJpaRepository: ReadingLevelAssessmentJpaRepository,
    private val childJpaRepository: ChildJpaRepository
) : ReadingLevelRepositoryPort {

    override fun findByChildId(childId: Long): ReadingLevel? =
        readingLevelJpaRepository.findByChildId(childId)?.toDomain()

    override fun save(readingLevel: ReadingLevel): ReadingLevel {
        val existing = readingLevelJpaRepository.findByChildId(readingLevel.childId)
        val child = existing?.child ?: childJpaRepository.getReferenceById(readingLevel.childId)
        return readingLevelJpaRepository.save(
            ReadingLevelEntity(
                id = existing?.id ?: 0,
                child = child,
                level = readingLevel.level,
                updatedAt = Instant.now()
            )
        ).toDomain()
    }

    override fun saveAssessment(assessment: ReadingLevelAssessment): ReadingLevelAssessment {
        val child = childJpaRepository.getReferenceById(assessment.childId)
        return assessmentJpaRepository.save(
            ReadingLevelAssessmentEntity(
                child = child,
                quizId = assessment.quizId,
                oldLevel = assessment.oldLevel,
                newLevel = assessment.newLevel,
                assessedAt = assessment.assessedAt
            )
        ).toDomain()
    }

    override fun findAssessmentsByChildId(childId: Long): List<ReadingLevelAssessment> =
        assessmentJpaRepository.findByChildIdOrderByAssessedAtDesc(childId).map { it.toDomain() }
}
