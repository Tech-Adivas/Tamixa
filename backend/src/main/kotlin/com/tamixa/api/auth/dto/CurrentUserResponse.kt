package com.tamixa.api.auth.dto

data class CurrentUserResponse(
    val email: String,
    val role: String,
    /** Display name shown across app when set; falls back to email/phone otherwise. */
    val nickname: String? = null,
    /** Optional display name (alias for nickname). */
    val displayName: String? = null,
    /** Permission names (e.g. MANAGE_STORIES, MODERATE_STORIES) for admin RBAC. Empty for PARENT. */
    val permissions: List<String> = emptyList(),
    /** Parent opt-in for cautious story-art personalization (synced across devices). */
    val storyArtPersonalizationOptIn: Boolean = false
)
