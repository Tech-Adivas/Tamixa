package com.tamixa.application.port.voice

/**
 * Self-hosted voice cloning (XTTS-style): synthesize text using a reference audio sample.
 *
 * Implementations are typically Python services running XTTS or similar models.
 */
interface SelfHostedVoiceCloningPort {

    /**
     * Synthesize speech using a reference audio sample.
     *
     * @param text Plain text (SSML should be stripped by caller)
     * @param referencePath Storage path or identifier for the reference audio
     * @param language BCP-47 language code for pronunciation
     * @return Encoded audio bytes (e.g. MP3) or null on failure
     */
    fun synthesize(text: String, referencePath: String, language: String): ByteArray?
}

