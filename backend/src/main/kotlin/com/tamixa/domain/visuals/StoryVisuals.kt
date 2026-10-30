package com.tamixa.domain.visuals

import java.time.Instant

/**
 * A library story's slideshow: a few AI pictures shown as timed camera shots while the story is narrated.
 * Planned once on the source-language text and shared by every language and voice.
 */
data class StoryVisualSet(
    val id: Long = 0,
    val storyId: Long,
    val status: StoryVisualSetStatus,
    val sourceLanguage: String,
    val sourceWordCount: Int = 0,
    val charactersJson: String? = null,
    val characterSheetPath: String? = null,
    val lastError: String? = null,
    val approvedAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    /** Children see the slideshow only when every picture is ready and an admin approved it. */
    val visibleToChildren: Boolean get() = status == StoryVisualSetStatus.READY && approvedAt != null
}

enum class StoryVisualSetStatus { PLANNING, GENERATING, READY, FAILED }

/** One generated base picture; several shots can show it with different framing. */
data class StoryVisualImage(
    val id: Long = 0,
    val setId: Long,
    val imageKey: String,
    val storyMoment: String?,
    val prompt: String,
    val subjectsJson: String?,
    val imagePath: String? = null,
    val status: StoryVisualImageStatus = StoryVisualImageStatus.PENDING,
    val lastError: String? = null,
)

enum class StoryVisualImageStatus { PENDING, READY, FAILED }

/** A camera shot on a picture, starting at [startWord] of the source text. */
data class StoryVisualShot(
    val id: Long = 0,
    val setId: Long,
    val shotIndex: Int,
    val imageKey: String,
    val narrationStart: String,
    val startWord: Int,
    val camera: String,
    val focus: String?,
    val movement: String,
)
