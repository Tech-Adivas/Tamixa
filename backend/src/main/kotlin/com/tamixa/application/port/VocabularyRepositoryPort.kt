package com.tamixa.application.port

import com.tamixa.domain.ChildVocabulary
import com.tamixa.domain.VocabularyWord

interface VocabularyRepositoryPort {
    fun saveWord(word: VocabularyWord): VocabularyWord
    fun findWordById(wordId: Long): VocabularyWord?
    fun findWordByWordAndLanguage(word: String, language: String): VocabularyWord?
    fun findWordsByLanguageAndDifficulty(language: String, difficultyLevel: Int): List<VocabularyWord>
    fun findAllWords(): List<VocabularyWord>
    fun saveChildVocabulary(childVocabulary: ChildVocabulary): ChildVocabulary
    fun findChildVocabularyByChildId(childId: Long): List<ChildVocabulary>
    fun findChildVocabularyByChildIdAndWordId(childId: Long, wordId: Long): ChildVocabulary?
    fun findChildVocabularyByChildIdAndMasteryLevel(childId: Long, minMasteryLevel: Int): List<ChildVocabulary>
    fun countLearnedWordsByChild(childId: Long): Int
    fun countMasteredWordsByChild(childId: Long, minMasteryLevel: Int): Int
    fun countAllWords(): Int
    fun findWordsByLanguage(language: String): List<VocabularyWord>
    fun countWordsByLanguage(language: String): Int
    fun findWordsByDifficulty(difficultyLevel: Int): List<VocabularyWord>
}