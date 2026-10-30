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
     * Synthesize with explicit voice [settings] instead of the cloned-voice ones, e.g. the story narrator's: a
     * professional library voice and a parent's instant clone need different stability/speed to sound natural.
     */
    fun synthesize(text: String, voiceId: String, language: String, settings: ElevenLabsVoiceSettings): ByteArray? =
        synthesize(text, voiceId, language)

    /**
     * Delete a cloned voice at ElevenLabs (frees one of the account's custom voice slots).
     * @return true when deleted or already gone, false on failure
     */
    fun deleteVoice(voiceId: String): Boolean

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

/**
 * ElevenLabs `voice_settings` for one request (each 0..1, speed 0.7..1.2; clamped when sent).
 * Stability: higher = steadier but flatter. Similarity: how closely to follow the voice's recording (too high also
 * copies its flaws). Speed: 1.0 = the voice's own pace.
 */
data class ElevenLabsVoiceSettings(
    val stability: Double,
    val similarityBoost: Double,
    val style: Double = 0.0,
    val speakerBoost: Boolean = true,
    val speed: Double = 1.0,
)
