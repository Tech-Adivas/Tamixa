package com.tamixa.application.port

/**
 * Redis cache for TTS output metadata.
 * Key: tts:{masterStoryId}:{language} -> audioFileUrl
 * Prevents duplicate TTS generation.
 */
interface TtsMetadataCachePort {

    fun getAudioUrl(masterStoryId: Long, language: String): String?

    fun setAudioUrl(masterStoryId: Long, language: String, audioFileUrl: String)

    fun hasAudio(masterStoryId: Long, language: String): Boolean

    /** Clear all TTS cache entries (e.g. tts:* keys in Redis). */
    fun flushAll()

    /** Clear cache for given story and languages. Call when regenerating so no stale URLs are served. */
    fun invalidateForStory(masterStoryId: Long, languages: List<String>)
}
