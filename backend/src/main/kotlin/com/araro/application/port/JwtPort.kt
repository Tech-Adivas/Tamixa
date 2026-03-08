package com.araro.application.port

data class TokenClaims(
    val email: String,
    val role: String
)

interface JwtPort {

    fun generateAccessToken(email: String, role: String): String

    fun generateRefreshToken(email: String, role: String): String

    fun validateAccessToken(token: String): TokenClaims?

    fun validateRefreshToken(token: String): TokenClaims?

    fun getAccessExpirationSeconds(): Long
}
