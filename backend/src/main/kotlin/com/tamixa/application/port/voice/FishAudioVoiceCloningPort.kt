package com.tamixa.application.port.voice

/**
 * Fish Audio voice cloning: create a TTS model from sample audio, synthesize with [reference_id].
 * See https://docs.fish.audio/api-reference/endpoint/model/create-model and .../text-to-speech
 */
interface FishAudioVoiceCloningPort {

    /**
     * Create a fast TTS voice model from a reference sample (POST /model).
     * @return Model `_id` used as `reference_id` in TTS, or null on failure
     */
    fun createModelFromSample(audioBytes: ByteArray, fileName: String, title: String): String?

    /**
     * Synthesize speech using a Fish model id as [reference_id].
     * @return MP3 bytes or null on failure
     */
    fun synthesize(text: String, modelId: String, language: String): ByteArray?

    fun hadRecentAuthFailure(modelId: String): Boolean

    fun hadRecentQuotaFailure(modelId: String): Boolean

    fun recentFailureMessage(modelId: String): String?
}
