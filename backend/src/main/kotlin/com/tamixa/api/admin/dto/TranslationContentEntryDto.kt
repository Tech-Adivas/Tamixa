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
    val moral: String? = null
)
