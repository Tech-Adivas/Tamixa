package com.tamixa.application.port

import com.tamixa.domain.GeneratedStoryScene
import com.tamixa.domain.narration.EmotionTaggedScript

interface GeneratedStorySceneRepositoryPort {

    /**
     * Save or replace scenes + segments for a generated story.
     * Uses scene extraction (---, ***), theme→backgroundHint, emotion tagging for narration/dialogue.
     */
    fun saveSceneWithSegments(
        storyId: Long,
        language: String,
        script: String,
        totalDurationSeconds: Int,
        audioUrl: String,
        theme: String? = null,
        emotionTaggedScript: EmotionTaggedScript? = null
    )

    /** Load all scenes with segments by story id and language; empty if none. */
    fun findScenesByStoryIdAndLanguage(storyId: Long, language: String): List<GeneratedStoryScene>
}
