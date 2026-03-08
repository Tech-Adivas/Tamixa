package com.araro.network

import com.araro.util.AraroLog
import com.araro.domain.AuthTokens
import com.araro.security.TokenStorage
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
                    AraroLog.d("HttpClient", message)
                }
            }
            level = io.ktor.client.plugins.logging.LogLevel.INFO
        }
    }
}
