package com.tamixa.application.narration

import com.tamixa.domain.narration.EmotionSegment
import com.tamixa.domain.narration.EmotionTag
import com.tamixa.domain.narration.EmotionTaggedScript

/**
 * Detects emotion per segment for SSML prosody mapping.
 *
 * Architecture: Deterministic rules first; optional AI assist in future.
 * No hallucinated content—only labels; original text preserved.
 */
interface EmotionTaggingService {

    /**
     * Tag script with emotion labels per segment.
     * @param scriptText Narration script (from formatter)
     * @param language Language code (ta, hi, te, kn, ml, en) for native interjection detection
     * @return EmotionTaggedScript with segments; original text unchanged
     */
    fun tagEmotions(scriptText: String, language: String = "en"): EmotionTaggedScript
}
