package com.tamixa.repository

import com.tamixa.domain.AuthTokens
import com.tamixa.domain.CurrentUser
import com.tamixa.network.AuthApi
import com.tamixa.security.TokenStorage

class AuthRepository(
    private val api: AuthApi,
    private val tokenStorage: TokenStorage
) {
    suspend fun login(email: String, password: String): Result<AuthTokens> = runCatching {
        val tokens = api.login(email, password)
        tokenStorage.saveTokens(
            tokens.accessToken,
            tokens.refreshToken,
            tokens.expiresInSeconds
        )
        tokens
    }

    suspend fun register(
        email: String,
        password: String,
        acceptedTerms: Boolean = false,
        acceptedPrivacy: Boolean = false,
        acceptedParentalAttestation: Boolean = false
    ): Result<AuthTokens> = runCatching {
        val tokens = api.register(email, password, acceptedTerms, acceptedPrivacy, acceptedParentalAttestation)
        tokenStorage.saveTokens(
            tokens.accessToken,
            tokens.refreshToken,
            tokens.expiresInSeconds
        )
        tokens
    }

    suspend fun requestPasswordlessCode(email: String): Result<Boolean> = runCatching {
        api.requestPasswordlessCode(email)
    }

    suspend fun verifyPasswordlessCode(
        email: String,
        code: String,
        acceptedTerms: Boolean = true,
        acceptedPrivacy: Boolean = true,
        acceptedParentalAttestation: Boolean = false
    ): Result<AuthTokens> = runCatching {
        val tokens = api.verifyPasswordlessCode(email, code, acceptedTerms, acceptedPrivacy, acceptedParentalAttestation)
        tokenStorage.saveTokens(
            tokens.accessToken,
            tokens.refreshToken,
            tokens.expiresInSeconds
        )
        tokens
    }

    suspend fun sendOtp(phone: String): Result<AuthApi.OtpSendResult> = runCatching {
        api.sendOtp(phone)
    }

    suspend fun loginWithOtp(phone: String, code: String): Result<AuthTokens> = runCatching {
        val tokens = api.loginWithOtp(phone, code)
        tokenStorage.saveTokens(
            tokens.accessToken,
            tokens.refreshToken,
            tokens.expiresInSeconds
        )
        tokens
    }

    fun isLoggedIn(): Boolean = tokenStorage.getAccessToken() != null

    fun getStoredTokens(): AuthTokens? {
        val access = tokenStorage.getAccessToken() ?: return null
        val refresh = tokenStorage.getRefreshToken() ?: return null
        val expiry = tokenStorage.getTokenExpiryMs() ?: return null
        return AuthTokens(
            accessToken = access,
            refreshToken = refresh,
            expiresInSeconds = (expiry / 1000)
        )
    }

    suspend fun me(): Result<CurrentUser> = runCatching { api.me() }

    suspend fun updateProfile(nickname: String?, displayName: String?): Result<Unit> = runCatching {
        api.updateProfile(nickname, displayName)
    }

    suspend fun updateStoryArtPersonalizationOptIn(optIn: Boolean): Result<Unit> = runCatching {
        api.updateStoryArtPersonalizationOptIn(optIn)
    }

    fun logout() {
        tokenStorage.clear()
    }

    suspend fun deleteAccount(): Result<Unit> = runCatching {
        api.deleteAccount()
        tokenStorage.clear()
    }
}
