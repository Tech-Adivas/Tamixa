package com.araro.repository

import com.araro.domain.AuthTokens
import com.araro.domain.CurrentUser
import com.araro.network.AuthApi
import com.araro.security.TokenStorage

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

    fun logout() {
        tokenStorage.clear()
    }
}
