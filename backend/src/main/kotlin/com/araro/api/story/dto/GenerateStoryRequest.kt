package com.araro.api.story.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class GenerateStoryRequest(
    @field:Min(1, message = "Age must be between 1 and 12")
    @field:Max(12, message = "Age must be between 1 and 12")
    val age: Int,

    @field:Size(max = 10, message = "Language code must be at most 10 characters")
    val language: String = "ta",

    @field:NotBlank(message = "Theme is required")
    @field:Size(max = 100)
    val theme: String,

    @field:NotBlank(message = "Child name is required")
    @field:Size(max = 255)
    val childName: String,

    val childId: Long? = null,

    /** Phase 2: CALM, SOOTHING, ADVENTUROUS, DEFAULT. Influences tone (e.g. bedtime). */
    @field:Size(max = 20)
    val emotionMode: String? = null,

    /** Phase 2: Parent instructions for story (e.g. "include a puppy", "set in a forest"). Sanitized server-side. */
    @field:Size(max = 200)
    val parentCustomPrompt: String? = null,

    /** Conversation messages (feelings, preferences) to tune the prompt. Sanitized and summarized server-side. */
    @field:Size(max = 10)
    val conversationMessages: List<String>? = null
)
