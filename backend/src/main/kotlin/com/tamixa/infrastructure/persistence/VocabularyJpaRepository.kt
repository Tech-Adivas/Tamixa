package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface VocabularyWordJpaRepository : JpaRepository<VocabularyWordEntity, Long> {
    fun findByWordAndLanguage(word: String, language: String): VocabularyWordEntity?
    fun findByLanguageAndDifficultyLevelOrderByWord(language: String, difficultyLevel: Int): List<VocabularyWordEntity>
}

interface ChildVocabularyJpaRepository : JpaRepository<ChildVocabularyEntity, Long> {

    @Query("SELECT cv FROM ChildVocabularyEntity cv WHERE cv.child.id = :childId ORDER BY cv.learnedAt DESC")
    fun findByChildId(@Param("childId") childId: Long): List<ChildVocabularyEntity>

    @Query("SELECT cv FROM ChildVocabularyEntity cv WHERE cv.child.id = :childId ORDER BY cv.learnedAt DESC")
    fun findByChildIdOrderByLearnedAtDesc(@Param("childId") childId: Long): List<ChildVocabularyEntity>

    @Query("SELECT cv FROM ChildVocabularyEntity cv WHERE cv.child.id = :childId AND cv.masteryLevel >= :masteryLevel")
    fun findByChildIdAndMasteryLevelGreaterThanEqual(
        @Param("childId") childId: Long,
        @Param("masteryLevel") masteryLevel: Int
    ): List<ChildVocabularyEntity>

    @Query("SELECT cv FROM ChildVocabularyEntity cv WHERE cv.child.id = :childId AND cv.word.id = :wordId")
    fun findByChildIdAndWordId(
        @Param("childId") childId: Long,
        @Param("wordId") wordId: Long
    ): ChildVocabularyEntity?

    @Query("SELECT CASE WHEN COUNT(cv) > 0 THEN true ELSE false END FROM ChildVocabularyEntity cv WHERE cv.child.id = :childId AND cv.word.id = :wordId")
    fun existsByChildIdAndWordId(@Param("childId") childId: Long, @Param("wordId") wordId: Long): Boolean

    @Query("SELECT COUNT(cv) FROM ChildVocabularyEntity cv WHERE cv.child.id = :childId")
    fun countByChildId(@Param("childId") childId: Long): Int

    @Query("SELECT COUNT(cv) FROM ChildVocabularyEntity cv WHERE cv.child.id = :childId AND cv.masteryLevel >= :masteryLevel")
    fun countByChildIdAndMasteryLevelGreaterThanEqual(
        @Param("childId") childId: Long,
        @Param("masteryLevel") masteryLevel: Int
    ): Int
}
