package com.tamixa.domain

import java.time.Instant

data class Quiz(
    val id: Long,
    val storyId: Long,
    val questions: List<QuizQuestion>,
    val difficultyLevel: Int,
    val createdAt: Instant
)

data class QuizQuestion(
    val id: String,
    val text: String,
    val type: QuestionType,
    val options: List<String>?,
    val correctAnswer: String,
    val explanation: String?
)

enum class QuestionType {
    MULTIPLE_CHOICE,
    TRUE_FALSE,
    SHORT_ANSWER
}

data class QuizResponse(
    val id: Long,
    val quizId: Long,
    val childId: Long,
    val answers: Map<String, String>,
    val score: Int,
    val maxScore: Int,
    val completedAt: Instant
) {
    val percentage: Int
        get() = if (maxScore > 0) (score * 100) / maxScore else 0
}
