package com.tamixa.application.port

import com.tamixa.domain.StoryScene
import com.tamixa.domain.narration.EmotionTaggedScript
import com.tamixa.domain.narration.EmotionTag

/**
 * Segment info for per-segment TTS. Order matches saveSceneWithSegments segment layout.
 */
data class SegmentInfoForTts(val text: String, val emotion: EmotionTag)

interface StorySceneRepositoryPort {

    /**
     * Compute flat segment list for per-segment TTS. Same order as saveSceneWithSegments.
     */
    fun computeSegmentInfosForTts(
        script: String,
        theme: String?,
        emotionTaggedScript: EmotionTaggedScript?
    ): List<SegmentInfoForTts>

    /**
     * Save or replace scenes + segments for a translation.
     * Uses scene extraction (---, ***), theme→backgroundHint, emotion tagging for narration/dialogue.
     * @param theme Optional; used to derive backgroundHint per scene
     * @param emotionTagged When provided, persists segment_type and speaker (Character for DIALOGUE)
     * @param segmentAudioUrls When non-null, use per-segment URLs (size must match total segments)
     */
    fun saveSceneWithSegments(
        translationId: Long,
        script: String,
        totalDurationSeconds: Int,
        audioUrl: String,
        theme: String? = null,
        emotionTaggedScript: EmotionTaggedScript? = null,
        segmentAudioUrls: List<String>? = null
    )

    /** Load all scenes with segments by translation id; empty if none. */
    fun findScenesByTranslationId(translationId: Long): List<StoryScene>

    /** @deprecated Use findScenesByTranslationId */
    fun findByTranslationId(translationId: Long): StoryScene? =
        findScenesByTranslationId(translationId).firstOrNull()

    /** Delete scenes and segments for a translation. Use when invalidating narration. */
    fun deleteByTranslationId(translationId: Long)

    /** Delete all scenes and segments. Use when clearing all narration (nuclear cleanup). */
    fun deleteAll(): Int
}
