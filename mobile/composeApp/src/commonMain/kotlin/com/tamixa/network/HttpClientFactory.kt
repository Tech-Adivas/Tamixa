package com.tamixa.network

import com.tamixa.util.TamixaLog
import com.tamixa.domain.AuthTokens
import com.tamixa.security.TokenStorage
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

@Serializable
private data class RefreshTokenBody(val refreshToken: String)

fun createKtorClient(
    baseUrl: String,
    tokenStorage: TokenStorage,
    onSessionExpired: (() -> Unit)? = null,
    enableLogging: Boolean = true
): HttpClient = HttpClient(ktorEngine) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        })
    }
    defaultRequest {
        url(baseUrl)
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.Json)
    }
    install(HttpTimeout) {
        connectTimeoutMillis = ApiConfig.CONNECT_TIMEOUT_MS
        socketTimeoutMillis = ApiConfig.SOCKET_TIMEOUT_MS
    }
    install(io.ktor.client.plugins.HttpRequestRetry) {
        retryOnServerErrors(maxRetries = ApiConfig.RETRY_MAX_ATTEMPTS)
        exponentialDelay()
    }
    install(Auth) {
        bearer {
            loadTokens {
                tokenStorage.getAccessToken()?.let { access ->
                    BearerTokens(access, tokenStorage.getRefreshToken() ?: "")
                }
            }
            refreshTokens {
                val refresh = tokenStorage.getRefreshToken() ?: return@refreshTokens null
                val refreshClient = HttpClient(ktorEngine) {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true; isLenient = true })
                    }
                    defaultRequest {
                        url(baseUrl)
                        contentType(ContentType.Application.Json)
                    }
                }
                try {
                    val tokens: AuthTokens = refreshClient.post("${ApiConfig.API_VERSION}/auth/refresh") {
                        setBody(RefreshTokenBody(refreshToken = refresh))
                    }.body()
                    tokenStorage.saveTokens(
                        tokens.accessToken,
                        tokens.refreshToken,
                        tokens.expiresInSeconds
                    )
                    BearerTokens(tokens.accessToken, tokens.refreshToken)
                } catch (e: Throwable) {
                    TamixaLog.w("HttpClient", "Token refresh failed, clearing session", e)
                    tokenStorage.clear()
                    onSessionExpired?.invoke()
                    null
                } finally {
                    refreshClient.close()
                }
            }
        }
    }
    if (enableLogging) {
        install(io.ktor.client.plugins.logging.Logging) {
            logger = object : io.ktor.client.plugins.logging.Logger {
                override fun log(message: String) {
                    if (message.contains("CancellationException") && message.contains("left the composition")) return
                    // Redact Authorization header so tokens are never logged
                    val sanitized = message
                        .replace(Regex("(Authorization|authorization):\\s*Bearer\\s+[^\\s\\]]+")) { "${it.groupValues[1]}: ***" }
                        .replace(Regex("Bearer\\s+[A-Za-z0-9_.-]+")) { "Bearer ***" }
                    TamixaLog.d("HttpClient", sanitized)
                }
            }
            level = io.ktor.client.plugins.logging.LogLevel.INFO
        }
    }
}
