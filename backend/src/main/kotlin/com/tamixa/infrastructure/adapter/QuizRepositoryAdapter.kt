package com.tamixa.infrastructure.adapter

import com.tamixa.application.port.QuizRepositoryPort
import com.tamixa.domain.Quiz
import com.tamixa.domain.QuizResponse
import com.tamixa.infrastructure.persistence.ChildJpaRepository
import com.tamixa.infrastructure.persistence.QuizEntity
import com.tamixa.infrastructure.persistence.QuizJpaRepository
import com.tamixa.infrastructure.persistence.QuizResponseEntity
import com.tamixa.infrastructure.persistence.QuizResponseJpaRepository
import com.tamixa.infrastructure.persistence.StoryJpaRepository
import org.springframework.stereotype.Repository

@Repository
class QuizRepositoryAdapter(
    private val quizJpaRepository: QuizJpaRepository,
    private val responseJpaRepository: QuizResponseJpaRepository,
    private val storyJpaRepository: StoryJpaRepository,
    private val childJpaRepository: ChildJpaRepository
) : QuizRepositoryPort {

    override fun findByStoryId(storyId: Long): Quiz? =
        quizJpaRepository.findByStoryId(storyId)?.toDomain()

    override fun save(quiz: Quiz): Quiz {
        val story = storyJpaRepository.findById(quiz.storyId)
            .orElseThrow { IllegalArgumentException("Story not found: ${quiz.storyId}") }
        val questionsMap = quiz.questions.map { q ->
            mapOf(
                "id" to q.id,
                "text" to q.text,
                "type" to q.type.name,
                "options" to (q.options ?: emptyList()),
                "correctAnswer" to q.correctAnswer,
                "explanation" to (q.explanation ?: "")
            )
        }
        return quizJpaRepository.save(
            QuizEntity(
                story = story,
                questions = questionsMap,
                difficultyLevel = quiz.difficultyLevel,
                createdAt = quiz.createdAt
            )
        ).toDomain()
    }

    override fun saveResponse(response: QuizResponse): QuizResponse {
        val quiz = quizJpaRepository.findById(response.quizId)
            .orElseThrow { IllegalArgumentException("Quiz not found: ${response.quizId}") }
        val child = childJpaRepository.getReferenceById(response.childId)
        return responseJpaRepository.save(
            QuizResponseEntity(
                quiz = quiz,
                child = child,
                answers = response.answers,
                score = response.score,
                maxScore = response.maxScore,
                completedAt = response.completedAt
            )
        ).toDomain()
    }

    override fun findResponsesByChildId(childId: Long, limit: Int): List<QuizResponse> =
        responseJpaRepository.findByChildIdOrderByCompletedAtDesc(childId)
            .take(limit)
            .map { it.toDomain() }

    override fun findResponsesByQuizId(quizId: Long): List<QuizResponse> =
        responseJpaRepository.findByQuizIdOrderByCompletedAtDesc(quizId)
            .map { it.toDomain() }
}
