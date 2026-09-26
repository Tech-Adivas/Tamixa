package com.tamixa.api.admin.dto

import jakarta.validation.constraints.Size

/**
 * Per-language translation content with optional title and moral.
 * Used when admin edits other-language content in the story form.
 */
data class TranslationContentEntryDto(
    @field:Size(max = 50_000)
    val content: String,

    @field:Size(max = 255)
    val title: String? = null,

    @field:Size(max = 500)
    val moral: String? = null,

    /** Null = leave unchanged; blank after trim clears per-locale override (inherit master). */
    @field:Size(max = 8000)
    val postStoryMission: String? = null,

    @field:Size(max = 512)
    val postStoryResourceUrl: String? = null,

    /** Null = leave unchanged; blank after trim clears per-locale value. */
    @field:Size(max = 4000)
    val parentContentNote: String? = null,

    /** Null = leave unchanged; empty list clears. */
    val parentDiscussionPrompts: List<String>? = null,

    @field:Size(max = 500)
    val speakAlongPrompt: String? = null,
)
