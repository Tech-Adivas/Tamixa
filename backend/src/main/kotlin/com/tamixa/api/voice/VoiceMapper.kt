package com.tamixa.api.voice

import com.tamixa.api.voice.dto.VoiceProfileResponse
import com.tamixa.domain.VoiceCloningJob
import com.tamixa.domain.VoiceProfile

fun VoiceProfile.toResponse(): VoiceProfileResponse = VoiceProfileResponse(
    id = id,
    parentId = parentId,
    createdAt = createdAt,
    heygenVoiceId = heygenVoiceId
)

/** Maps a voice cloning job to the same response shape (upload returns job; list/get return profile). */
fun VoiceCloningJob.toResponse(): VoiceProfileResponse = VoiceProfileResponse(
    id = id,
    parentId = parentId,
    createdAt = createdAt
)
