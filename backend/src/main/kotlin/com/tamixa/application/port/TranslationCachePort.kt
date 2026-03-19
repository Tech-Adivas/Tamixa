package com.tamixa.application.port

/**
 * Cache for translation results. Prevents duplicate translation API calls.
 * Content-hash key: same source (title+content+moral) → same translation; invalidates when story is edited.
 */
interface TranslationCachePort {

    fun get(masterStoryId: Long, language: String): CachedTranslation?

    fun set(masterStoryId: Long, language: String, content: CachedTranslation)

    fun hasTranslation(masterStoryId: Long, language: String): Boolean

    /** Get cached translation by source content hash. Key: sourceHash + targetLang. */
    fun getByContentHash(sourceHash: String, targetLang: String): CachedTranslation?

    /** Cache translation by source content hash. */
    fun setByContentHash(sourceHash: String, targetLang: String, content: CachedTranslation)
}

data class CachedTranslation(
    val title: String? = null,
    val content: String = "",
    val moral: String? = null,
    val wordCount: Int = 0,
    val readingTimeMinutes: Double = 0.0
)
