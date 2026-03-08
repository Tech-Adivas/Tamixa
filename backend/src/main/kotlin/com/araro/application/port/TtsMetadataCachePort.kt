package com.araro.application.port

/**
 * Redis cache for TTS output metadata.
 * Key: tts:{masterStoryId}:{language} -> audioFileUrl
 * Prevents duplicate TTS generation.
 */
interface TtsMetadataCachePort {

    fun getAudioUrl(masterStoryId: Long, language: String): String?

    fun setAudioUrl(masterStoryId: Long, language: String, audioFileUrl: String)

    fun hasAudio(masterStoryId: Long, language: String): Boolean
}
