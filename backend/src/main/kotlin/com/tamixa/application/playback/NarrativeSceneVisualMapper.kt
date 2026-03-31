package com.tamixa.application.playback

import com.tamixa.api.stream.dto.NarrativeSceneVisualDto

fun PlaybackManifest.toNarrativeSceneVisualDtos(): List<NarrativeSceneVisualDto> {
    if (scenes.isEmpty()) return emptyList()
    val total = totalDurationMs.coerceAtLeast(1L)
    var cumulativeMs = 0L
    return scenes.mapIndexed { idx, scene ->
        val startProgress = (cumulativeMs.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
        cumulativeMs += scene.segments.sumOf { it.durationMs }
        NarrativeSceneVisualDto(
            sceneIndex = idx,
            startProgress = startProgress,
            illustrationUrl = scene.illustrationImageUrl,
            backgroundHint = scene.backgroundHint
        )
    }
}
