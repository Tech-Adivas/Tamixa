package com.tamixa.application.port.narration

/**
 * Forced alignment: finds where each word of a known transcript is spoken in an audio file.
 * Used for read-along sync, so the app highlights the word the narrator is actually saying.
 */
interface SpeechAlignmentPort {

    /** Words in spoken order with start/end times in seconds, or null when alignment is unavailable or failed. */
    fun alignWords(audio: ByteArray, transcript: String): List<AlignedWord>?
}

data class AlignedWord(
    val text: String,
    val startSec: Double,
    val endSec: Double
)
