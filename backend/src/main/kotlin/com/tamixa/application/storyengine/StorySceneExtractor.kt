package com.tamixa.application.storyengine

import org.springframework.stereotype.Component

/**
 * Stage 1: Normalize story — extract scenes from raw script.
 * Splits by scene markers (---, ***); derives backgroundHint from theme.
 */
@Component
class StorySceneExtractor {

    /** Scene break patterns: --- or *** on own line. */
    private val sceneBreakRegex = Regex("\\n[-]{2,}\\n|\\n[*]{2,}\\n")

    /**
     * Split script into scene chunks. If no markers, single scene.
     */
    fun extractScenes(script: String): List<SceneChunk> {
        val trimmed = script.trim()
        if (trimmed.isBlank()) return listOf(SceneChunk("", 0))
        val chunks = trimmed.split(sceneBreakRegex).map { it.trim() }.filter { it.isNotBlank() }
        return if (chunks.isEmpty()) listOf(SceneChunk(trimmed, 0))
        else chunks.mapIndexed { i, text -> SceneChunk(text, i) }
    }

    /**
     * Map theme/category to background hint for visuals.
     */
    fun themeToBackgroundHint(theme: String?): String? {
        if (theme.isNullOrBlank()) return null
        val t = theme.trim().lowercase()
        return when {
            t.contains("adventure") || t.contains("forest") -> "forest_adventure"
            t.contains("nature") || t.contains("garden") -> "garden_day"
            t.contains("village") || t.contains("home") -> "village_morning"
            t.contains("sea") || t.contains("ocean") || t.contains("beach") -> "beach_sunset"
            t.contains("night") || t.contains("moon") || t.contains("star") -> "night_stars"
            t.contains("animal") || t.contains("pet") -> "meadow_animals"
            t.contains("family") || t.contains("friend") -> "home_cozy"
            else -> t.replace(Regex("[^a-z0-9]"), "_").take(64).ifBlank { null }
        }
    }
}

data class SceneChunk(val text: String, val sceneIndex: Int)
