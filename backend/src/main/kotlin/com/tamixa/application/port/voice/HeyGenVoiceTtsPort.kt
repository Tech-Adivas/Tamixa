package com.tamixa.application.port.voice

/**
 * HeyGen voice TTS: synthesize text using an existing HeyGen voice_id.
 * Voice must be created in HeyGen app (Instant Voice Cloning); we use POST /v2/voices/{voice_id}/preview for TTS.
 */
interface HeyGenVoiceTtsPort {
    fun synthesize(text: String, voiceId: String, language: String): ByteArray?
}
