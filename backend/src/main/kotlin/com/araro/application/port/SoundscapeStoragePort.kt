package com.araro.application.port

/**
 * Storage for soundscape audio files.
 * Path convention: soundscapes/{soundscapeId}.mp3
 */
interface SoundscapeStoragePort {
    fun upload(soundscapeId: Long, audioBytes: ByteArray, contentType: String = "audio/mpeg"): String
    fun delete(storagePath: String): Unit
    fun exists(storagePath: String): Boolean
}
