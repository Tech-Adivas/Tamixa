package com.araro.api.stream.dto

/**
 * Word-level timing for transcript sync (karaoke-style highlight).
 * When TTS or voice transcription provides timings, the app can sync the highlight to the voice.
 */
data class WordTimingDto(
    val word: String,
    val startSec: Double,
    val endSec: Double
)
