package com.araro.domain

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
    val role: String
)
