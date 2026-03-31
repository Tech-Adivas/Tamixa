package com.tamixa.application.service

import com.tamixa.application.port.QuizRepositoryPort
import com.tamixa.domain.Quiz
import com.tamixa.domain.QuizQuestion
import com.tamixa.domain.QuizResponse
import com.tamixa.domain.QuestionType
import com.tamixa.domain.Story
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class QuizService(
    private val quizRepository: QuizRepositoryPort,
    private val readingLevelService: ReadingLevelService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun generateQuizForStory(story: Story): Quiz {
        log.debug("Generating quiz for story={}", story.id)
        
        // Extract key concepts from story content (simplified)
        val questions = generateQuestionsFromStory(story)
        
        val quiz = Quiz(
            id = 0,
            storyId = story.id,
            questions = questions,
            difficultyLevel = calculateDifficulty(story.age),
            createdAt = Instant.now()
        )
        
        return quizRepository.save(quiz)
    }

    fun getOrGenerateQuizForStory(story: Story): Quiz {
        return quizRepository.findByStoryId(story.id)
            ?: generateQuizForStory(story)
    }

    fun submitQuizResponse(quizId: Long, childId: Long, answers: Map<String, String>): QuizResponse {
        val quiz = quizRepository.findByStoryId(quizId)
            ?: throw IllegalArgumentException("Quiz not found: $quizId")
        
        val score = calculateScore(quiz, answers)
        val response = QuizResponse(
            id = 0,
            quizId = quizId,
            childId = childId,
            answers = answers,
            score = score,
            maxScore = 100,
            completedAt = Instant.now()
        )
        
        val saved = quizRepository.saveResponse(response)
        
        // Update reading level based on quiz performance
        readingLevelService.assessAndUpdateLevel(childId, score, 100, quizId)
        
        log.info("Quiz submitted: child={} quiz={} score={}", childId, quizId, score)
        return saved
    }

    fun getQuizResultsForChild(childId: Long, limit: Int = 20): List<QuizResponse> {
        return quizRepository.findResponsesByChildId(childId, limit)
    }

    private fun generateQuestionsFromStory(story: Story): List<QuizQuestion> {
        // Simplified question generation - in production, use OpenAI
        val questions = mutableListOf<QuizQuestion>()
        
        // Question 1: Main character
        questions.add(
            QuizQuestion(
                id = UUID.randomUUID().toString(),
                text = "Who is the main character in this story?",
                type = QuestionType.SHORT_ANSWER,
                options = null,
                correctAnswer = story.childName,
                explanation = "The story is about ${story.childName}."
            )
        )
        
        // Question 2: Theme
        questions.add(
            QuizQuestion(
                id = UUID.randomUUID().toString(),
                text = "What is the theme of this story?",
                type = QuestionType.MULTIPLE_CHOICE,
                options = listOf(story.theme, "Adventure", "Mystery", "Comedy"),
                correctAnswer = story.theme,
                explanation = "The theme of the story is ${story.theme}."
            )
        )
        
        // Question 3: Comprehension
        questions.add(
            QuizQuestion(
                id = UUID.randomUUID().toString(),
                text = "Did you enjoy this story?",
                type = QuestionType.TRUE_FALSE,
                options = listOf("True", "False"),
                correctAnswer = "True",
                explanation = "We hope you enjoyed the story!"
            )
        )
        
        return questions
    }

    private fun calculateScore(quiz: Quiz, answers: Map<String, String>): Int {
        var correct = 0
        for (question in quiz.questions) {
            val userAnswer = answers[question.id]?.trim()?.lowercase()
            val correctAnswer = question.correctAnswer.trim().lowercase()
            if (userAnswer == correctAnswer) {
                correct++
            }
        }
        return if (quiz.questions.isNotEmpty()) (correct * 100) / quiz.questions.size else 0
    }

    private fun calculateDifficulty(age: Int): Int {
        return when {
            age < 5 -> 1
            age < 8 -> 2
            age < 12 -> 3
            else -> 4
        }
    }
}
