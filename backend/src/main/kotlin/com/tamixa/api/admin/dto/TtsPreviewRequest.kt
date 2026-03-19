package com.tamixa.api.admin.dto

import jakarta.validation.constraints.Size

/**
 * Request for admin TTS preview: direct TTS (content as-is) or prompt + TTS.
 * - language: target language code (e.g. ta, en, hi).
 * - prompt: optional. If blank or null → direct TTS. If set → transform story with this prompt via LLM, then TTS.
 */
data class TtsPreviewRequest(
    @field:Size(max = 10)
    val language: String = "ta",
    @field:Size(max = 2000)
    val prompt: String? = null
)
