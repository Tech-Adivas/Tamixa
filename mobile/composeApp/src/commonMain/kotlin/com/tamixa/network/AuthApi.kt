package com.tamixa.network

import com.tamixa.domain.AuthTokens
import com.tamixa.domain.CurrentUser
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

/** Matches backend ErrorResponse for proper 4xx/5xx error message extraction. */
@Serializable
internal data class ApiErrorResponse(
    val message: String,
    val status: Int,
    val traceId: String? = null,
    val timestamp: String? = null
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

    suspend fun me(): CurrentUser =
        client.get("${ApiConfig.API_VERSION}/auth/me").body()

    /** Update profile (nickname, displayName). Shown instead of email/phone across the app. */
    suspend fun updateProfile(nickname: String?, displayName: String?) {
        client.patch("${ApiConfig.API_VERSION}/auth/profile") {
            setBody(UpdateProfileRequest(nickname = nickname, displayName = displayName))
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
