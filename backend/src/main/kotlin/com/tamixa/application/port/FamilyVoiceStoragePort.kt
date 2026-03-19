package com.tamixa.application.port

/**
 * Stores family recorded voice in private path.
 * Path: families/{parentId}/stories/{storyId}/{language}.mp3 or .m4a
 */
interface FamilyVoiceStoragePort {
    fun upload(storyId: Long, parentId: Long, language: String, audioBytes: ByteArray, contentType: String = "audio/mpeg", fileExt: String = "mp3"): String
    fun delete(storagePath: String)
    fun exists(storagePath: String): Boolean
}
