package com.tamixa.network

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

// ── DTOs ──────────────────────────────────────────────────────────────────────

@Serializable
data class ReadingLevelDto(
    val childId: Long,
    val level: Int,
    val updatedAt: String
)

@Serializable
data class ReadingStreakDto(
    val childId: Long,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastReadAt: String? = null,
    val isActive: Boolean
)

@Serializable
data class VocabularyWordDto(
    val id: Long,
    val word: String,
    val definition: String,
    val language: String,
    val difficultyLevel: Int,
    val exampleSentence: String? = null
)

@Serializable
data class ChildVocabularyDto(
    val childId: Long,
    val wordId: Long,
    val word: String,
    val definition: String,
    val masteryLevel: Int,
    val learnedAt: String
)

@Serializable
data class VocabularyProgressDto(
    val totalWords: Int,
    val wordsLearned: Int,
    val wordsMastered: Int,
    val progressPercentage: Int
)

@Serializable
data class LearnWordRequest(
    val childId: Long,
    val wordId: Long,
    val masteryLevel: Int = 1
)

@Serializable
data class ClassroomDto(
    val id: Long,
    val teacherId: Long,
    val name: String,
    val code: String,
    val subject: String? = null,
    val gradeLevel: String? = null
)

@Serializable
data class JoinClassroomRequest(
    val code: String,
    val childId: Long
)

@Serializable
data class QuizDto(
    val id: Long,
    val storyId: Long,
    val questions: List<QuizQuestionDto>,
    val difficultyLevel: Int
)

@Serializable
data class QuizQuestionDto(
    val id: String,
    val text: String,
    val type: String,
    val options: List<String>? = null
)

@Serializable
data class QuizResultDto(
    val quizId: Long,
    val score: Int,
    val maxScore: Int,
    val percentage: Int
)

@Serializable
data class SubmitQuizRequest(
    val answers: Map<String, String>
)

// ── API client ────────────────────────────────────────────────────────────────

class EducationApi(private val client: HttpClient) {

    // Reading level
    suspend fun getReadingLevel(childId: Long): ReadingLevelDto =
        client.get("${ApiConfig.API_VERSION}/reading-levels/$childId").requireBodyOrThrow()

    // Reading streak
    suspend fun getStreak(childId: Long): ReadingStreakDto =
        client.get("${ApiConfig.API_VERSION}/reading-streaks/$childId").requireBodyOrThrow()

    suspend fun recordRead(childId: Long): ReadingStreakDto =
        client.post("${ApiConfig.API_VERSION}/reading-streaks/$childId/record-read").requireBodyOrThrow()

    // Vocabulary
    suspend fun getVocabularyProgress(childId: Long): VocabularyProgressDto =
        client.get("${ApiConfig.API_VERSION}/vocabulary/child/$childId/progress").requireBodyOrThrow()

    suspend fun getChildVocabulary(childId: Long): List<ChildVocabularyDto> =
        client.get("${ApiConfig.API_VERSION}/vocabulary/child/$childId/words").bodyIfSuccess() ?: emptyList()

    suspend fun getWordSuggestions(childId: Long, language: String? = null, limit: Int = 10): List<VocabularyWordDto> =
        client.get("${ApiConfig.API_VERSION}/vocabulary/child/$childId/suggestions") {
            language?.let { parameter("language", it) }
            parameter("limit", limit)
        }.bodyIfSuccess() ?: emptyList()

    suspend fun markWordLearned(childId: Long, wordId: Long, masteryLevel: Int = 1): ChildVocabularyDto =
        client.post("${ApiConfig.API_VERSION}/vocabulary/learn") {
            contentType(ContentType.Application.Json)
            setBody(LearnWordRequest(childId, wordId, masteryLevel))
        }.requireBodyOrThrow()

    // Classrooms
    suspend fun getChildClassrooms(childId: Long): List<ClassroomDto> =
        client.get("${ApiConfig.API_VERSION}/teachers/child/$childId/classrooms").bodyIfSuccess() ?: emptyList()

    suspend fun joinClassroom(code: String, childId: Long): ClassroomDto =
        client.post("${ApiConfig.API_VERSION}/teachers/classrooms/join") {
            contentType(ContentType.Application.Json)
            setBody(JoinClassroomRequest(code, childId))
        }.requireBodyOrThrow()

    // Quiz
    suspend fun getQuizForStory(storyId: Long): QuizDto =
        client.get("${ApiConfig.API_VERSION}/quizzes/stories/$storyId").requireBodyOrThrow()

    suspend fun submitQuiz(quizId: Long, childId: Long, answers: Map<String, String>): QuizResultDto =
        client.post("${ApiConfig.API_VERSION}/quizzes/$quizId/submit") {
            contentType(ContentType.Application.Json)
            parameter("childId", childId)
            setBody(SubmitQuizRequest(answers))
        }.requireBodyOrThrow()
}
