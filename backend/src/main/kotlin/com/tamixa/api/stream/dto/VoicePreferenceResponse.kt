package com.tamixa.api.stream.dto

data class VoicePreferenceResponse(
    val voiceProfile: String,
    val playbackMode: String = "default"
)
