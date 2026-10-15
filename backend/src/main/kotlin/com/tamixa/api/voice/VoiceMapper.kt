package com.tamixa.api.voice

import com.tamixa.api.voice.dto.VoiceProfileResponse
import com.tamixa.application.voice.originalReferenceFileName
import com.tamixa.domain.VoiceProfile

fun VoiceProfile.toResponse(): VoiceProfileResponse = VoiceProfileResponse(
    id = id,
    parentId = parentId,
    createdAt = createdAt,
    profileName = profileDisplayName(),
    heygenVoiceId = heygenVoiceId,
    referenceFileName = originalReferenceFileName(referenceAudioPath)
)

private fun VoiceProfile.profileDisplayName(): String? {
    val path = referenceAudioPath?.trim().orEmpty()
    if (path.isBlank()) return null
    // Cloning uploads are stored as "cloning_{ts}_ref_{name}"; show only the name.
    val fileName = path.substringAfterLast('/').substringBeforeLast('.').trim()
        .replace(Regex("^cloning_\\d+_ref_"), "")
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
