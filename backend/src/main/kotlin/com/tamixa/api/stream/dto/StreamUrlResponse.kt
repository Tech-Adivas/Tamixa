package com.tamixa.api.stream.dto

/**
 * Stream URL and optional word-level timings for transcript sync.
 * [avatarStatus] NONE | IMAGE_ONLY | VIDEO_GENERATING | VIDEO_READY | VIDEO_FAILED for UI (e.g. "Avatar video is being prepared").
 * [voiceFallback] true when cloned voice was requested but default audio is served (TTS unavailable).
 */
data class StreamUrlResponse(
    val streamUrl: String,
    val avatarUrl: String? = null,
    val avatarVideoUrl: String? = null,
    val avatarStatus: String? = null,
    val voiceFallback: Boolean = false,
    val wordTimings: List<WordTimingDto>? = null,
    /** Actual audio duration in seconds from story_narration_audio. Use for progress bar instead of readingTimeMinutes. */
    val durationSeconds: Int? = null,
    /** In-player narrative visuals (multi-scene timelines, optional per-scene art). */
    val narrativeScenes: List<NarrativeSceneVisualDto>? = null,
    /**
     * Optional Phase 4: muted supplementary clip (e.g. synthetic host). Null when not configured.
     * Client should play story audio as primary and keep this video muted.
     */
    val hostStoryClipUrl: String? = null
)
