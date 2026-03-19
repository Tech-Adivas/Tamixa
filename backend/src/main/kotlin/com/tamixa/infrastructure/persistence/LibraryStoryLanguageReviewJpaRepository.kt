package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface LibraryStoryLanguageReviewJpaRepository : JpaRepository<LibraryStoryLanguageReviewEntity, Long> {

    fun findByLibraryStoryId(libraryStoryId: Long): List<LibraryStoryLanguageReviewEntity>

    fun findByLibraryStoryIdIn(libraryStoryIds: List<Long>): List<LibraryStoryLanguageReviewEntity>

    fun deleteByLibraryStoryId(libraryStoryId: Long)

    fun deleteByLibraryStoryIdAndLanguage(libraryStoryId: Long, language: String)
}
