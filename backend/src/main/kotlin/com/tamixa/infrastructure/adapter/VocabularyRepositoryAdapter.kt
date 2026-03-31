package com.tamixa.infrastructure.adapter

import com.tamixa.application.port.VocabularyRepositoryPort
import com.tamixa.domain.ChildVocabulary
import com.tamixa.domain.VocabularyWord
import com.tamixa.infrastructure.persistence.ChildJpaRepository
import com.tamixa.infrastructure.persistence.ChildVocabularyEntity
import com.tamixa.infrastructure.persistence.ChildVocabularyJpaRepository
import com.tamixa.infrastructure.persistence.VocabularyWordEntity
import com.tamixa.infrastructure.persistence.VocabularyWordJpaRepository
import org.springframework.stereotype.Repository

@Repository
class VocabularyRepositoryAdapter(
    private val vocabularyWordJpaRepository: VocabularyWordJpaRepository,
    private val childVocabularyJpaRepository: ChildVocabularyJpaRepository,
    private val childJpaRepository: ChildJpaRepository
) : VocabularyRepositoryPort {

    override fun saveWord(word: VocabularyWord): VocabularyWord =
        vocabularyWordJpaRepository.save(
            VocabularyWordEntity(
                id = word.id,
                word = word.word,
                definition = word.definition,
                language = word.language,
                difficultyLevel = word.difficultyLevel,
                exampleSentence = word.exampleSentence,
                createdAt = word.createdAt
            )
        ).toDomain()

    override fun findWordById(wordId: Long): VocabularyWord? =
        vocabularyWordJpaRepository.findById(wordId).orElse(null)?.toDomain()

    override fun findWordByWordAndLanguage(word: String, language: String): VocabularyWord? =
        vocabularyWordJpaRepository.findByWordAndLanguage(word, language)?.toDomain()

    override fun findWordsByLanguageAndDifficulty(language: String, difficultyLevel: Int): List<VocabularyWord> =
        vocabularyWordJpaRepository.findByLanguageAndDifficultyLevelOrderByWord(language, difficultyLevel)
            .map { it.toDomain() }

    override fun findAllWords(): List<VocabularyWord> =
        vocabularyWordJpaRepository.findAll().map { it.toDomain() }

    override fun saveChildVocabulary(childVocabulary: ChildVocabulary): ChildVocabulary {
        val child = childJpaRepository.getReferenceById(childVocabulary.childId)
        val word = vocabularyWordJpaRepository.findById(childVocabulary.wordId)
            .orElseThrow { IllegalArgumentException("Vocabulary word not found: ${childVocabulary.wordId}") }
        return childVocabularyJpaRepository.save(
            ChildVocabularyEntity(
                id = childVocabulary.id,
                child = child,
                word = word,
                masteryLevel = childVocabulary.masteryLevel,
                learnedAt = childVocabulary.learnedAt
            )
        ).toDomain()
    }

    override fun findChildVocabularyByChildId(childId: Long): List<ChildVocabulary> =
        childVocabularyJpaRepository.findByChildId(childId).map { it.toDomain() }

    override fun findChildVocabularyByChildIdAndWordId(childId: Long, wordId: Long): ChildVocabulary? =
        childVocabularyJpaRepository.findByChildIdAndWordId(childId, wordId)?.toDomain()

    override fun findChildVocabularyByChildIdAndMasteryLevel(childId: Long, minMasteryLevel: Int): List<ChildVocabulary> =
        childVocabularyJpaRepository.findByChildIdAndMasteryLevelGreaterThanEqual(childId, minMasteryLevel)
            .map { it.toDomain() }

    override fun countLearnedWordsByChild(childId: Long): Int =
        childVocabularyJpaRepository.countByChildId(childId)

    override fun countMasteredWordsByChild(childId: Long, minMasteryLevel: Int): Int =
        childVocabularyJpaRepository.countByChildIdAndMasteryLevelGreaterThanEqual(childId, minMasteryLevel)

    override fun countAllWords(): Int =
        vocabularyWordJpaRepository.count().toInt()

    override fun findWordsByLanguage(language: String): List<VocabularyWord> =
        vocabularyWordJpaRepository.findAll().filter { it.language == language }.map { it.toDomain() }

    override fun countWordsByLanguage(language: String): Int =
        vocabularyWordJpaRepository.findAll().count { it.language == language }

    override fun findWordsByDifficulty(difficultyLevel: Int): List<VocabularyWord> =
        vocabularyWordJpaRepository.findAll().filter { it.difficultyLevel == difficultyLevel }.map { it.toDomain() }
}
