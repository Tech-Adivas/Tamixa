package com.tamixa.repository

import com.tamixa.network.ChildVocabularyDto
import com.tamixa.network.ClassroomDto
import com.tamixa.network.EducationApi
import com.tamixa.network.QuizDto
import com.tamixa.network.QuizResultDto
import com.tamixa.network.ReadingLevelDto
import com.tamixa.network.ReadingStreakDto
import com.tamixa.network.VocabularyProgressDto
import com.tamixa.network.VocabularyWordDto

class EducationRepository(private val api: EducationApi) {

    suspend fun getReadingLevel(childId: Long): Result<ReadingLevelDto> = runCatching {
        api.getReadingLevel(childId)
    }

    suspend fun getStreak(childId: Long): Result<ReadingStreakDto> = runCatching {
        api.getStreak(childId)
    }

    suspend fun recordRead(childId: Long): Result<ReadingStreakDto> = runCatching {
        api.recordRead(childId)
    }

    suspend fun getVocabularyProgress(childId: Long): Result<VocabularyProgressDto> = runCatching {
        api.getVocabularyProgress(childId)
    }

    suspend fun getChildVocabulary(childId: Long): Result<List<ChildVocabularyDto>> = runCatching {
        api.getChildVocabulary(childId)
    }

    suspend fun getWordSuggestions(childId: Long, language: String? = null, limit: Int = 10): Result<List<VocabularyWordDto>> = runCatching {
        api.getWordSuggestions(childId, language, limit)
    }

    suspend fun markWordLearned(childId: Long, wordId: Long, masteryLevel: Int = 1): Result<ChildVocabularyDto> = runCatching {
        api.markWordLearned(childId, wordId, masteryLevel)
    }

    suspend fun getChildClassrooms(childId: Long): Result<List<ClassroomDto>> = runCatching {
        api.getChildClassrooms(childId)
    }

    suspend fun joinClassroom(code: String, childId: Long): Result<ClassroomDto> = runCatching {
        api.joinClassroom(code, childId)
    }

    suspend fun getQuizForStory(storyId: Long): Result<QuizDto> = runCatching {
        api.getQuizForStory(storyId)
    }

    suspend fun submitQuiz(quizId: Long, childId: Long, answers: Map<String, String>): Result<QuizResultDto> = runCatching {
        api.submitQuiz(quizId, childId, answers)
    }
}
