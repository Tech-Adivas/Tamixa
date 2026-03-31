package com.tamixa.api.voice

import com.tamixa.api.voice.dto.VoiceProfileResponse
import com.tamixa.domain.VoiceCloningJob
import com.tamixa.domain.VoiceProfile

fun VoiceProfile.toResponse(): VoiceProfileResponse = VoiceProfileResponse(
    id = id,
    parentId = parentId,
    createdAt = createdAt,
    profileName = profileDisplayName(),
    heygenVoiceId = heygenVoiceId
)

/** Maps a voice cloning job to the same response shape (upload returns job; list/get return profile). */
fun VoiceCloningJob.toResponse(): VoiceProfileResponse = VoiceProfileResponse(
    id = id,
    parentId = parentId,
    createdAt = createdAt,
    profileName = voiceName
)

private fun VoiceProfile.profileDisplayName(): String? {
    val path = referenceAudioPath?.trim().orEmpty()
    if (path.isBlank()) return null
    val fileName = path.substringAfterLast('/').substringBeforeLast('.').trim()
    if (fileName.isBlank()) return null
    return fileName
        .replace('-', ' ')
        .replace('_', ' ')
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString(" ") { token ->
            token.replaceFirstChar { c -> c.uppercaseChar() }
        }
}
