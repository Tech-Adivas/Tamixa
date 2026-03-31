package com.tamixa.application.port

import com.tamixa.domain.Quiz
import com.tamixa.domain.QuizResponse

interface QuizRepositoryPort {
    fun findByStoryId(storyId: Long): Quiz?
    fun save(quiz: Quiz): Quiz
    fun saveResponse(response: QuizResponse): QuizResponse
    fun findResponsesByChildId(childId: Long, limit: Int = 20): List<QuizResponse>
    fun findResponsesByQuizId(quizId: Long): List<QuizResponse>
}
