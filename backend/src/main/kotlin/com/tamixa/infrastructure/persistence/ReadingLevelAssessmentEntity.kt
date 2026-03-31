package com.tamixa.infrastructure.persistence

import com.tamixa.domain.ReadingLevelAssessment
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
@Table(name = "reading_level_assessments")
class ReadingLevelAssessmentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    val child: ChildEntity,

    @Column(name = "quiz_id")
    val quizId: Long? = null,

    @Column(name = "old_level", nullable = false)
    val oldLevel: Int,

    @Column(name = "new_level", nullable = false)
    val newLevel: Int,

    @Column(name = "assessed_at", nullable = false)
    val assessedAt: Instant = Instant.now()
) {
    fun toDomain() = ReadingLevelAssessment(
        id = id,
        childId = child.id,
        quizId = quizId,
        oldLevel = oldLevel,
        newLevel = newLevel,
        assessedAt = assessedAt
    )
}
