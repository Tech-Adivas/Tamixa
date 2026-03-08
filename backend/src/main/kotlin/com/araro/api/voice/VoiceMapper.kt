package com.araro.api.voice

import com.araro.api.voice.dto.VoiceProfileResponse
import com.araro.domain.VoiceCloningJob
import com.araro.domain.VoiceProfile

fun VoiceProfile.toResponse(): VoiceProfileResponse = VoiceProfileResponse(
    id = id,
    parentId = parentId,
    createdAt = createdAt
)

/** Maps a voice cloning job to the same response shape (upload returns job; list/get return profile). */
fun VoiceCloningJob.toResponse(): VoiceProfileResponse = VoiceProfileResponse(
    id = id,
    parentId = parentId,
    createdAt = createdAt
)
