package com.tamixa.network

import com.tamixa.domain.AuthTokens
import com.tamixa.domain.CurrentUser
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import com.tamixa.util.TamixaLog
import kotlinx.serialization.Serializable

/** Matches backend ErrorResponse for proper 4xx/5xx error message extraction. */
@Serializable
internal data class ApiErrorResponse(
    val message: String = "",
    val status: Int = 0,
    val traceId: String? = null,
    val timestamp: String? = null,
    /** Stable code from e.g. [ApiBadRequestException] on POST /stories/generate. */
    val code: String? = null,
)

class AuthApiException(val statusCode: Int, message: String) : RuntimeException(message)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class PasswordlessRequest(val email: String)

@Serializable
data class MobileOtpRequest(val phone: String)

@Serializable
data class VerifyOtpRequest(val phone: String, val code: String)

@Serializable
internal data class PasswordlessVerifyRequest(
    val email: String,
    val code: String,
    val acceptedTerms: Boolean = false,
    val acceptedPrivacy: Boolean = false,
    val acceptedParentalAttestation: Boolean = false
)

@Serializable
internal data class PasswordlessSentResponse(val sent: Boolean)

@Serializable
internal data class OtpSentResponse(val sent: Boolean, val code: String? = null)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val acceptedTerms: Boolean = false,
    val acceptedPrivacy: Boolean = false,
    val acceptedParentalAttestation: Boolean = false
)

@Serializable
data class ProfileParentJson(
    val id: Long,
    val email: String,
    val nickname: String? = null,
    val displayName: String? = null,
)

@Serializable
data class ProfileChildJson(
    val id: Long,
    val name: String,
    val dateOfBirth: String,
    val languagePreference: String? = null,
)

@Serializable
data class ProfileBootResponse(
    val parent: ProfileParentJson,
    val children: List<ProfileChildJson> = emptyList(),
    val subscriptionPlan: String? = null,
)

class AuthApi(private val client: HttpClient) {

    private suspend fun parseAuthResponse(response: HttpResponse): AuthTokens {
        if (!response.status.isSuccess()) {
            val msg = try {
                response.body<ApiErrorResponse>().message
            } catch (_: Exception) {
                "Authentication failed (${response.status})"
            }
            throw AuthApiException(response.status.value, msg)
        }
        return response.body()
    }

    suspend fun login(email: String, password: String): AuthTokens =
        parseAuthResponse(
            client.post("${ApiConfig.API_VERSION}/auth/login") {
                setBody(LoginRequest(email, password))
            }
        )

    suspend fun register(
        email: String,
        password: String,
        acceptedTerms: Boolean = false,
        acceptedPrivacy: Boolean = false,
        acceptedParentalAttestation: Boolean = false
    ): AuthTokens =
        parseAuthResponse(
            client.post("${ApiConfig.API_VERSION}/auth/register") {
                setBody(RegisterRequest(email, password, acceptedTerms, acceptedPrivacy, acceptedParentalAttestation))
            }
        )

    suspend fun refresh(refreshToken: String): AuthTokens =
        parseAuthResponse(
            client.post("${ApiConfig.API_VERSION}/auth/refresh") {
                setBody(RefreshTokenRequest(refreshToken))
            }
        )

    suspend fun me(): CurrentUser {
        val resp = client.get("${ApiConfig.API_VERSION}/auth/me")
        resp.bodyIfSuccess<CurrentUser>()?.let { return it }
        throw ClientRequestException(resp, runCatching { resp.bodyAsText() }.getOrElse { "" })
    }

    /** Parent + children (same contract as web GET /profile). */
    suspend fun getProfile(): ProfileBootResponse? =
        try {
            val response = client.get("${ApiConfig.API_VERSION}/profile")
            when (response.status.value) {
                in 200..299 -> response.body()
                else -> null
            }
        } catch (e: Exception) {
            TamixaLog.w("AuthApi", "getProfile failed", e)
            null
        }

    /** Update profile (nickname, displayName). Shown instead of email/phone across the app. */
    suspend fun updateProfile(nickname: String?, displayName: String?) {
        val response = client.patch("${ApiConfig.API_VERSION}/auth/profile") {
            setBody(UpdateProfileRequest(nickname = nickname, displayName = displayName))
        }
        if (!response.status.isSuccess()) {
            val msg = try {
                response.body<ApiErrorResponse>().message
            } catch (_: Exception) {
                "Profile update failed (${response.status})"
            }
            throw AuthApiException(response.status.value, msg.ifBlank { "Profile update failed (${response.status})" })
        }
    }

    suspend fun updateStoryArtPersonalizationOptIn(optIn: Boolean) {
        val response = client.patch("${ApiConfig.API_VERSION}/auth/me/story-art-personalization") {
            contentType(ContentType.Application.Json)
            setBody(StoryArtPersonalizationRequest(optIn = optIn))
        }
        if (!response.status.isSuccess()) {
            val msg = try {
                response.body<ApiErrorResponse>().message
            } catch (_: Exception) {
                "Update failed (${response.status})"
            }
            throw AuthApiException(response.status.value, msg)
        }
    }

    /** Step 1: Request passwordless code sent to email. Returns sent status. */
    suspend fun requestPasswordlessCode(email: String): Boolean =
        client.post("${ApiConfig.API_VERSION}/auth/passwordless") {
            setBody(PasswordlessRequest(email))
        }.body<PasswordlessSentResponse>().sent

    /** Step 2: Verify code and get tokens. For new users, acceptedTerms, acceptedPrivacy and acceptedParentalAttestation are required. */
    suspend fun verifyPasswordlessCode(
        email: String,
        code: String,
        acceptedTerms: Boolean = true,
        acceptedPrivacy: Boolean = true,
        acceptedParentalAttestation: Boolean = false
    ): AuthTokens =
        parseAuthResponse(
            client.post("${ApiConfig.API_VERSION}/auth/passwordless/verify") {
                setBody(PasswordlessVerifyRequest(email, code, acceptedTerms, acceptedPrivacy, acceptedParentalAttestation))
            }
        )

    suspend fun sendOtp(phone: String): OtpSendResult =
        client.post("${ApiConfig.API_VERSION}/auth/otp/send") {
            setBody(MobileOtpRequest(phone))
        }.body<OtpSentResponse>().let { OtpSendResult(it.sent, it.code) }

    data class OtpSendResult(val sent: Boolean, val devCode: String? = null)

    suspend fun loginWithOtp(phone: String, code: String): AuthTokens =
        parseAuthResponse(
            client.post("${ApiConfig.API_VERSION}/auth/otp/verify") {
                setBody(VerifyOtpRequest(phone, code))
            }
        )

    /** Permanently delete the authenticated account and all data (GDPR). */
    suspend fun deleteAccount() {
        val response = client.delete("${ApiConfig.API_VERSION}/auth/account")
        if (!response.status.isSuccess()) {
            val msg = try {
                response.body<ApiErrorResponse>().message
            } catch (_: Exception) {
                "Account deletion failed (${response.status})"
            }
            throw AuthApiException(response.status.value, msg)
        }
    }
}

@Serializable
private data class RefreshTokenRequest(val refreshToken: String)

@Serializable
internal data class UpdateProfileRequest(
    val nickname: String? = null,
    val displayName: String? = null
)

@Serializable
private data class StoryArtPersonalizationRequest(val optIn: Boolean)
