package com.tamixa.api.education.dto

import com.tamixa.domain.Quiz
import com.tamixa.domain.QuizQuestion
import com.tamixa.domain.QuizResponse
import com.tamixa.domain.QuestionType
import jakarta.validation.constraints.NotEmpty
import java.time.Instant

data class QuizQuestionResponse(
    val id: String,
    val text: String,
    val type: String,
    val options: List<String>?,
    val explanation: String?
) {
    companion object {
        fun from(domain: QuizQuestion) = QuizQuestionResponse(
            id = domain.id,
            text = domain.text,
            type = domain.type.name,
            options = domain.options,
            explanation = domain.explanation
        )
    }
}

data class QuizResponse(
    val id: Long,
    val storyId: Long,
    val questions: List<QuizQuestionResponse>,
    val difficultyLevel: Int,
    val createdAt: Instant
) {
    companion object {
        fun from(domain: Quiz) = QuizResponse(
            id = domain.id,
            storyId = domain.storyId,
            questions = domain.questions.map { QuizQuestionResponse.from(it) },
            difficultyLevel = domain.difficultyLevel,
            createdAt = domain.createdAt
        )
    }
}

data class SubmitQuizRequest(
    @field:NotEmpty(message = "Answers cannot be empty")
    val answers: Map<String, String>
)

data class QuizResultResponse(
    val quizId: Long,
    val score: Int,
    val maxScore: Int,
    val percentage: Int,
    val completedAt: Instant
) {
    companion object {
        fun from(domain: com.tamixa.domain.QuizResponse) = QuizResultResponse(
            quizId = domain.quizId,
            score = domain.score,
            maxScore = domain.maxScore,
            percentage = domain.percentage,
            completedAt = domain.completedAt
        )
    }
}

data class QuizResultsPageResponse(
    val content: List<QuizResultResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val first: Boolean,
    val last: Boolean
)
