package com.tamixa.common

/**
 * Canonical job types for async AI/media workflows.
 * Used by processing_job table and job consumers.
 */
enum class JobType(val value: String) {
    STORY_POLISH_JOB("STORY_POLISH_JOB"),
    STORY_TRANSLATION_JOB("STORY_TRANSLATION_JOB"),
    STORY_TTS_JOB("STORY_TTS_JOB"),
    STORY_PIPELINE("STORY_PIPELINE"),
    VOICE_CLONE_JOB("VOICE_CLONE_JOB"),
    TALKING_VIDEO_JOB("TALKING_VIDEO_JOB"),
    SHARE_CLIP_JOB("SHARE_CLIP_JOB");

    override fun toString(): String = value

    companion object {
        fun fromString(s: String?): JobType? = entries.find { it.value.equals(s, ignoreCase = true) }
    }
}
