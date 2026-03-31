package com.tamixa.infrastructure.persistence

import com.tamixa.domain.Quiz
import com.tamixa.domain.QuizQuestion
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
@Table(name = "quizzes")
class QuizEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    val story: StoryEntity,

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    val questions: List<Map<String, Any>> = emptyList(),

    @Column(name = "difficulty_level", nullable = false)
    val difficultyLevel: Int = 1,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    fun toDomain(): Quiz {
        val parsedQuestions = questions.map { q ->
            QuizQuestion(
                id = q["id"] as String,
                text = q["text"] as String,
                type = com.tamixa.domain.QuestionType.valueOf(q["type"] as String),
                options = (q["options"] as? List<*>)?.map { it.toString() },
                correctAnswer = q["correctAnswer"] as String,
                explanation = q["explanation"] as? String
            )
        }
        return Quiz(
            id = id,
            storyId = story.id,
            questions = parsedQuestions,
            difficultyLevel = difficultyLevel,
            createdAt = createdAt
        )
    }
}
