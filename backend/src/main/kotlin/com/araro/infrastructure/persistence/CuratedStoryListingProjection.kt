package com.araro.infrastructure.persistence

import java.time.Instant

/**
 * JPA projection for story listing (avoids loading content).
 */
interface CuratedStoryListingProjection {
    fun getId(): Long
    fun getTitle(): String?
    fun getTheme(): String
    fun getLanguage(): String
    fun getAge(): Int
    fun getChildName(): String
    fun getWordCount(): Int
    fun getReadingTimeMinutes(): Double
    fun getAudioFileUrl(): String?
    fun getStatus(): String
    fun getCoverImageUrl(): String?
    fun getCoverVideoUrl(): String?
    fun getCreatedAt(): Instant
}
