package com.tamixa.api.story.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

data class GenerateStoryRequest(
    @field:Min(1, message = "Age must be between 1 and 99")
    @field:Max(99, message = "Age must be between 1 and 99")
    val age: Int,

    @field:Size(max = 10, message = "Language code must be at most 10 characters")
    val language: String = "ta",

    /** Free-text theme; optional when [generationTopicId] is set (server resolves theme). */
    @field:Size(max = 100)
    val theme: String? = null,

    /** When set, server uses a curated topic (Tamil theme string); skips free-text blocklist on theme. */
    @field:Size(max = 64)
    val generationTopicId: String? = null,

    /** Optional; when blank or null, a default listener name is used. */
    @field:Size(max = 255)
    val childName: String? = null,

    val childId: Long? = null,

    /** Phase 2: CALM, SOOTHING, ADVENTUROUS, DEFAULT. Influences tone (e.g. bedtime). */
    @field:Size(max = 20)
    val emotionMode: String? = null,

    /** Phase 2: Parent instructions for story (e.g. "include a puppy", "set in a forest"). Sanitized server-side. */
    @field:Size(max = 200)
    val parentCustomPrompt: String? = null,

    /** Conversation messages (feelings, preferences) to tune the prompt. Sanitized and summarized server-side. */
    @field:Size(max = 10)
    val conversationMessages: List<String>? = null,

    /** Optional educational focus: empathy, problem_solving, vocabulary, curiosity, perseverance, sharing, honesty, courage, kindness, friendship, responsibility, public_speaking, money_literacy, research_skills. Ignored if not in allowlist. */
    @field:Size(max = 30)
    val learningFocus: String? = null
)
