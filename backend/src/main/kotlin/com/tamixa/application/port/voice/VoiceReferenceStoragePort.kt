package com.tamixa.application.port.voice

/**
 * Storage for raw reference audio uploaded by parents for voice cloning.
 *
 * Implementations should persist the bytes (e.g. S3) and return a stable storage path
 * that can be used later by self-hosted voice cloning or ElevenLabs.
 */
interface VoiceReferenceStoragePort {

    /**
     * Store reference audio for a parent.
     *
     * @param parentId Parent identifier (used for path scoping)
     * @param fileName Original filename (used for extension/diagnostics)
     * @param bytes Audio bytes (e.g. WAV/MP3)
     * @return Storage path (e.g. voices/{parentId}/{profileId}/reference.wav)
     */
    fun storeReferenceAudio(parentId: Long, fileName: String, bytes: ByteArray): String

    /**
     * Read reference audio bytes by storage path (e.g. for creating ElevenLabs voice on first use).
     * @param path Storage path returned from storeReferenceAudio
     * @return Audio bytes or null if not found / not supported
     */
    fun getReferenceAudio(path: String): ByteArray?

    /**
     * Remove stored reference audio (e.g. when admin deletes a voice profile to re-upload).
     * No-op if path is blank or implementation has nothing to delete.
     */
    fun deleteReferenceAudio(path: String?) {}
}
