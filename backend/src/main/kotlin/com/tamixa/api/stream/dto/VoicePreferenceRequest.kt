package com.tamixa.api.stream.dto

import jakarta.validation.constraints.Size

data class VoicePreferenceRequest(
    @field:Size(max = 100)
    val voiceProfile: String = "default",
    @field:Size(max = 20)
    val playbackMode: String = "default"
)
