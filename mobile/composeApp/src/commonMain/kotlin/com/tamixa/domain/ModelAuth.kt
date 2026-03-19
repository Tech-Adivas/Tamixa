package com.tamixa.domain

import kotlinx.serialization.Serializable

@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long
)

@Serializable
data class CurrentUser(
    val email: String,
    val role: String,
    val nickname: String? = null,
    val displayName: String? = null
) {
    /** Display name shown across the app: nickname > displayName > email part (not synthetic) > fallback */
    fun displayNameOrFallback(fallback: String): String {
        val nick = nickname?.trim()?.takeIf { it.isNotBlank() }
        if (nick != null) return nick.replaceFirstChar { c -> c.uppercase() }
        val disp = displayName?.trim()?.takeIf { it.isNotBlank() }
        if (disp != null) return disp.replaceFirstChar { c -> c.uppercase() }
        val emailPart = email.substringBefore('@').takeIf { it.isNotBlank() }
        if (emailPart != null && !email.endsWith("@otp.tamixa.in")) {
            return emailPart.replaceFirstChar { c -> c.uppercase() }
        }
        return fallback
    }
}
