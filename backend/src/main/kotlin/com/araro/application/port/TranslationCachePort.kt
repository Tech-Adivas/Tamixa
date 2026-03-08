package com.araro.application.port

/**
 * Redis cache for translation results.
 * Key: translation:{masterStoryId}:{language}
 * Prevents duplicate translation API calls.
 */
interface TranslationCachePort {

    fun get(masterStoryId: Long, language: String): CachedTranslation?

    fun set(masterStoryId: Long, language: String, content: CachedTranslation)

    fun hasTranslation(masterStoryId: Long, language: String): Boolean
}

data class CachedTranslation(
    val title: String? = null,
    val content: String = "",
    val moral: String? = null,
    val wordCount: Int = 0,
    val readingTimeMinutes: Double = 0.0
)
