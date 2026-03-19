package com.tamixa.infrastructure.persistence

import java.time.Instant

/**
 * JPA projection for translation listing (avoids loading content, moral).
 */
interface StoryTranslationListingProjection {
    fun getId(): Long
    fun getMasterStoryId(): Long
    fun getTitle(): String?
    fun getLanguage(): String
    fun getWordCount(): Int
    fun getReadingTimeMinutes(): Double
    fun getCreatedAt(): Instant
}
