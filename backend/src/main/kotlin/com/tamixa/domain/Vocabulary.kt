package com.tamixa.domain

import java.time.Instant

data class VocabularyWord(
    val id: Long,
    val word: String,
    val definition: String,
    val language: String,
    val difficultyLevel: Int,
    val exampleSentence: String?,
    val createdAt: Instant
)

data class ChildVocabulary(
    val id: Long,
    val childId: Long,
    val wordId: Long,
    val masteryLevel: Int,
    val learnedAt: Instant
) {
    companion object {
        const val MASTERY_LEARNING = 0
        const val MASTERY_FAMILIAR = 1
        const val MASTERY_PROFICIENT = 2
        const val MASTERY_EXPERT = 3
    }
}
