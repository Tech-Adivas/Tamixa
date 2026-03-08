package com.araro.application.narration

/**
 * Text-to-Speech: converts SSML to audio bytes.
 * Uses Azure or Google Neural TTS with 10s timeout, max 2 retries.
 */
interface TTSService {

    /**
     * Synthesize SSML to MP3.
     * @param ssml Valid SSML string
     * @param language BCP-47 code
     * @param voiceProfile Voice identifier (e.g. en-US-Standard-A)
     * @return MP3 byte array or null on failure after retries
     */
    fun synthesize(ssml: String, language: String, voiceProfile: String): ByteArray?
}
