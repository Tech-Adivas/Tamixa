package com.tamixa.api.admin.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateLibraryStoryRequest(
    @field:Size(max = 255)
    val title: String? = null,

    @field:NotBlank(message = "Content is required")
    @field:Size(max = 50_000)
    val content: String,

    @field:NotBlank(message = "Category/theme is required")
    @field:Size(max = 100)
    val theme: String,

    /** Content category (distinct from theme). When null, theme is used. */
    @field:Size(max = 100)
    val category: String? = null,

    @field:Size(max = 10)
    val language: String = "ta",

    @field:Min(1, message = "Age must be between 1 and 12")
    @field:Max(12, message = "Age must be between 1 and 12")
    val age: Int,

    @field:Size(max = 255)
    val childName: String = "Child",

    @field:Size(max = 500)
    val moral: String? = null,

    @field:Size(max = 512)
    val audioFileUrl: String? = null,

    @field:Size(max = 20)
    val status: String = "DRAFT",

    @field:Size(max = 512)
    val coverImageUrl: String? = null,

    @field:Size(max = 512)
    val coverVideoUrl: String? = null,

    /** CALM, SOOTHING, ADVENTUROUS. Default CALM for narration tone. */
    @field:Size(max = 20)
    val emotionMode: String? = null,

    /** When false and status=PUBLISHED: update narrated content only, no pipeline. First-time publish uses true. */
    val regenerateNarration: Boolean? = null,

    /** Optional per-language content (e.g. "hi" -> "...", "en" -> "..."). When editing without prompt, use this to update other languages; source (ta) comes from content. Deprecated: prefer translationContentEntries. */
    val translationContents: Map<String, String>? = null,

    /** Per-language content with optional title and moral. When set, used instead of translationContents. */
    val translationContentEntries: Map<String, TranslationContentEntryDto>? = null
)
