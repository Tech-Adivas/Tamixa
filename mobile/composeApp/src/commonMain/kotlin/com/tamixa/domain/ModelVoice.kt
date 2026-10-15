package com.tamixa.domain

import kotlinx.serialization.Serializable

@Serializable
data class VoiceProfile(
    val id: Long,
    val parentId: Long,
    val createdAt: String,
    /** File name the recording was uploaded with (e.g. "my_voice_1791546447445.m4a"); matches the local copy. */
    val referenceFileName: String? = null
)

/**
 * Upload file name of this voice. Older servers send the stored name with a "cloning_{ts}_ref_" prefix
 * (e.g. "cloning_1791637796207_ref_my_voice_1791637793404.m4a"); the prefix is dropped so it matches the local file.
 */
fun VoiceProfile.uploadedFileName(): String? =
    referenceFileName?.replace(Regex("^cloning_\\d+_ref_"), "")?.takeIf { it.isNotBlank() }

/** Upload refused because the parent already has the maximum number of voices (server responds 409). */
class VoiceLimitReachedException(message: String) : Exception(message)

/** Outcome of deleting a voice on the server. */
enum class VoiceDeleteResult {
    /** Removed from the server (recording in storage and the profile). */
    DELETED,
    /** Not on the server (already deleted or never there); nothing left to do. */
    NOT_FOUND,
    /** Kept: a generated story still uses this voice. */
    IN_USE
}
