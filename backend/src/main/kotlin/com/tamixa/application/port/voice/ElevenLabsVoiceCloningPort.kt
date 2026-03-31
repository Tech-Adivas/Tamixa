package com.tamixa.application.port.voice

/**
 * ElevenLabs voice cloning: create voice from sample, synthesize text with that voice.
 * Used when parent uploads audio sample for voice cloning.
 */
interface ElevenLabsVoiceCloningPort {

    /**
     * Create a cloned voice from audio sample(s).
     * @param audioBytes MP3 or similar audio
     * @param fileName Original filename for content-type inference
     * @param name Display name for the voice
     * @return ElevenLabs voice_id, or null on failure
     */
    fun addVoice(audioBytes: ByteArray, fileName: String, name: String): String?

    /**
     * Synthesize text to speech using a cloned voice.
     * @param text Plain text (SSML stripped)
     * @param voiceId ElevenLabs voice_id
     * @param language BCP-47 language code for pronunciation
     * @return MP3 bytes or null on failure
     */
    fun synthesize(text: String, voiceId: String, language: String): ByteArray?

    /**
     * True when this voice recently received provider-auth failures (for example HTTP 401).
     * Helps admin surfaces show actionable status without digging through logs.
     */
    fun hadRecentAuthFailure(voiceId: String): Boolean

    /**
     * True when this voice recently failed due to ElevenLabs quota/credits exhaustion.
     */
    fun hadRecentQuotaFailure(voiceId: String): Boolean

    /**
     * Human-readable recent provider failure detail, if available.
     */
    fun recentFailureMessage(voiceId: String): String?
}
