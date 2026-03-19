package com.tamixa.application.port.voice

/**
 * Google Cloud Chirp 3 Instant Custom Voice: create voice from reference + consent, synthesize TTS.
 * Requires VOICE_CLONING_PROVIDER=google and GOOGLE_CLOUD_TTS_API_KEY.
 */
interface GoogleCloudVoiceCloningPort {

    /**
     * Create a voice cloning key from reference and consent audio.
     * @param consentAudioBytes Consent statement: "I am the owner of this voice and I consent..."
     * @return Voice cloning key string, or null on failure.
     */
    fun createVoiceCloningKey(
        referenceAudioBytes: ByteArray,
        consentAudioBytes: ByteArray,
        languageCode: String
    ): String?

    /**
     * Synthesize text to speech using a voice cloning key.
     * @param voiceCloningKey Key from createVoiceCloningKey
     */
    fun synthesize(text: String, voiceCloningKey: String, language: String): ByteArray?
}
