package com.araro.application.narration

/**
 * Stores narration audio in S3.
 * Path: stories/{storyId}/{language}/{voiceProfileSlug}.mp3 (multi-voice).
 * Legacy: voiceProfile "default" maps to v1.mp3 for backward compatibility.
 * Returns public CDN path (not raw storage path).
 */
interface AudioStorageService {

    /**
     * Upload MP3 bytes and return CDN-accessible URL.
     * @param storyId Master story ID
     * @param language Language code
     * @param voiceProfile Voice identifier; "default" → v1.mp3 for backward compat
     * @param mp3Bytes MP3 content
     * @return Public CDN path for streaming
     */
    fun uploadNarrationAudio(
        storyId: Long,
        language: String,
        voiceProfile: String,
        mp3Bytes: ByteArray
    ): String
}
