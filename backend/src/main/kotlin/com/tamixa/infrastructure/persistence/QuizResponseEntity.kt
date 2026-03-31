package com.tamixa.infrastructure.persistence

import com.tamixa.domain.QuizResponse
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "quiz_responses")
class QuizResponseEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    val quiz: QuizEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    val child: ChildEntity,

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    val answers: Map<String, String> = emptyMap(),

    @Column(nullable = false)
    val score: Int,

    @Column(name = "max_score", nullable = false)
    val maxScore: Int = 100,

    @Column(name = "completed_at", nullable = false)
    val completedAt: Instant = Instant.now()
) {
    fun toDomain() = QuizResponse(
        id = id,
        quizId = quiz.id,
        childId = child.id,
        answers = answers,
        score = score,
        maxScore = maxScore,
        completedAt = completedAt
    )
}
