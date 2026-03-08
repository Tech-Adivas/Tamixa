package com.araro.security

/**
 * Platform-specific secure token storage.
 * Android: EncryptedSharedPreferences
 * iOS: Keychain
 */
interface TokenStorage {
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun getTokenExpiryMs(): Long?
    fun saveTokens(accessToken: String, refreshToken: String, expiresInSeconds: Long)
    fun clear()
}
