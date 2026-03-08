package com.araro.application.port.narration

/**
 * Low-level TTS client (Azure/Google). Abstraction for neural TTS.
 */
interface TtsClientPort {

    /**
     * Synthesize SSML to MP3 bytes.
     * @return MP3 byte array or null on failure
     */
    fun synthesizeToMp3(ssml: String, language: String, voiceProfile: String): ByteArray?
}
