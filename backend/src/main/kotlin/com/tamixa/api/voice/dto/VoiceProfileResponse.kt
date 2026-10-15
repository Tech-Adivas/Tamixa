package com.tamixa.api.voice.dto

import java.time.Instant

data class VoiceProfileResponse(
    val id: Long,
    val parentId: Long,
    val createdAt: Instant,
    val profileName: String? = null,
    val heygenVoiceId: String? = null,
    /**
     * File name the recording was uploaded with (e.g. "my_voice_1791546447445.m4a"), without the server's
     * "cloning_{ts}_ref_" storage prefix; lets the app match its local copy.
     */
    val referenceFileName: String? = null
)
