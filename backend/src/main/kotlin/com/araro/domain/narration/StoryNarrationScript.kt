package com.araro.domain.narration

import java.time.Instant

/**
 * AI-enhanced conversational script for a translated story.
 * Links to story_translations for curated content pipeline.
 */
data class StoryNarrationScript(
    val id: Long,
    val translationId: Long,
    val toneMode: ToneMode,
    val scriptText: String,
    val safetyScore: Int,
    val createdAt: Instant
)
