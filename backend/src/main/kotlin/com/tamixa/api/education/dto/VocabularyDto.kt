package com.tamixa.api.education.dto

import com.tamixa.domain.ChildVocabulary
import com.tamixa.domain.VocabularyWord
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.Instant

data class VocabularyWordResponse(
    val id: Long,
    val word: String,
    val definition: String,
    val language: String,
    val difficultyLevel: Int,
    val exampleSentence: String?,
    val createdAt: Instant
) {
    companion object {
        fun from(domain: VocabularyWord) = VocabularyWordResponse(
            id = domain.id,
            word = domain.word,
            definition = domain.definition,
            language = domain.language,
            difficultyLevel = domain.difficultyLevel,
            exampleSentence = domain.exampleSentence,
            createdAt = domain.createdAt
        )
    }
}

data class ChildVocabularyResponse(
    val childId: Long,
    val wordId: Long,
    val word: String,
    val definition: String,
    val masteryLevel: Int,
    val learnedAt: Instant
) {
    companion object {
        fun from(domain: ChildVocabulary, word: VocabularyWord) = ChildVocabularyResponse(
            childId = domain.childId,
            wordId = domain.wordId,
            word = word.word,
            definition = word.definition,
            masteryLevel = domain.masteryLevel,
            learnedAt = domain.learnedAt
        )
    }
}

data class VocabularyProgressResponse(
    val totalWords: Int,
    val wordsLearned: Int,
    val wordsMastered: Int,
    val progressPercentage: Int
)

data class LearnWordRequest(
    @field:NotNull(message = "Child ID is required")
    @field:Positive(message = "Child ID must be positive")
    val childId: Long,

    @field:NotNull(message = "Word ID is required")
    @field:Positive(message = "Word ID must be positive")
    val wordId: Long,
    
    @field:NotNull(message = "Mastery level is required")
    val masteryLevel: Int = ChildVocabulary.MASTERY_FAMILIAR
)

data class CreateWordRequest(
    @field:NotBlank(message = "Word is required")
    val word: String,
    
    @field:NotBlank(message = "Definition is required")
    val definition: String,
    
    @field:NotBlank(message = "Language is required")
    val language: String,
    
    @field:NotNull(message = "Difficulty level is required")
    @field:Positive(message = "Difficulty level must be positive")
    val difficultyLevel: Int = 1,
    
    val exampleSentence: String? = null
)

data class VocabularyWordsPageResponse(
    val content: List<VocabularyWordResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val first: Boolean,
    val last: Boolean
)

data class MasteryLevelsResponse(
    val masteryLevels: Map<Int, Int>
)

data class VocabularyStatsResponse(
    val totalWords: Int,
    val languages: Map<String, Int>,
    val difficultyDistribution: Map<Int, Int>
)
