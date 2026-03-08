package com.araro.application.narration

import com.araro.domain.narration.ToneMode

/**
 * Maps story emotion_mode (from story generation) to narration ToneMode.
 * CALM/SOOTHING → CALM (bedtime); ADVENTUROUS → EXPRESSIVE (engaging).
 */
object EmotionToToneMapper {

    fun toToneMode(emotionMode: String?): ToneMode {
        if (emotionMode.isNullOrBlank()) return ToneMode.CALM
        return when (emotionMode.trim().uppercase()) {
            "ADVENTUROUS" -> ToneMode.EXPRESSIVE
            "CALM", "SOOTHING", "DEFAULT" -> ToneMode.CALM
            else -> ToneMode.CALM
        }
    }
}
