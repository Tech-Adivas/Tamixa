package com.tamixa.application.narration

import com.tamixa.domain.narration.EmotionTaggedScript
import com.tamixa.domain.narration.ToneMode

/**
 * Converts plain text narration to Speech Synthesis Markup Language (SSML).
 * Adds prosody, breaks, and lang for neural TTS engines.
 *
 * Two modes:
 * - Plain: buildSSML(scriptText, ...) - existing, no emotion layer
 * - Emotional: buildSSMLFromEmotionTagged(...) - segments with prosody per emotion
 */
interface SSMLBuilderService {

    /**
     * Build SSML string from plain script (legacy / non-emotion path).
     * @param scriptText Formatted narration text
     * @param language BCP-47 (e.g. en-US, hi-IN)
     * @param age For rate: slow if age < 6
     * @param toneMode Influences rate variation
     */
    fun buildSSML(
        scriptText: String,
        language: String,
        age: Int,
        toneMode: ToneMode
    ): String

    /**
     * Build SSML from emotion-tagged script with prosody per segment.
     * Uses age-based pacing, language tuning, and dialogue pitch differentiation.
     * Rejects if word count deviates too much (validation done by caller).
     */
    fun buildSSMLFromEmotionTagged(
        emotionTagged: EmotionTaggedScript,
        language: String,
        age: Int,
        toneMode: ToneMode
    ): String
}
