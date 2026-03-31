package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LibraryStoryLanguageReviewJpaRepository : JpaRepository<LibraryStoryLanguageReviewEntity, Long> {

    fun findByLibraryStoryId(libraryStoryId: Long): List<LibraryStoryLanguageReviewEntity>

    fun findByLibraryStoryIdAndLanguageIgnoreCase(libraryStoryId: Long, language: String): LibraryStoryLanguageReviewEntity?

    fun findByLibraryStoryIdIn(libraryStoryIds: List<Long>): List<LibraryStoryLanguageReviewEntity>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM LibraryStoryLanguageReviewEntity e WHERE e.libraryStoryId = :libraryStoryId")
    fun deleteByLibraryStoryId(@Param("libraryStoryId") libraryStoryId: Long): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        "DELETE FROM LibraryStoryLanguageReviewEntity e WHERE e.libraryStoryId = :libraryStoryId AND LOWER(e.language) = LOWER(:language)"
    )
    fun deleteByLibraryStoryIdAndLanguage(
        @Param("libraryStoryId") libraryStoryId: Long,
        @Param("language") language: String
    ): Int
}
