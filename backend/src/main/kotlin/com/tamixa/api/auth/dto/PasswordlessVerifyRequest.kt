package com.tamixa.api.auth.dto

import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Size

data class PasswordlessVerifyRequest(
    @field:Size(max = 255, message = "Email must not exceed 255 characters")
    val email: String? = null,

    @field:Size(max = 20, message = "Code must not exceed 20 characters")
    val code: String? = null,

    /** Opaque token from magic-link URL (32 hex chars, UUID without dashes). */
    @field:Size(max = 64, message = "Login token must not exceed 64 characters")
    val loginToken: String? = null,

    val acceptedTerms: Boolean = false,
    val acceptedPrivacy: Boolean = false,
    val acceptedParentalAttestation: Boolean = false
) {
    @get:AssertTrue(message = "Provide either loginToken or both email and code")
    val isValidPasswordlessPayload: Boolean
        get() {
            val t = loginToken?.trim().orEmpty()
            val e = email?.trim().orEmpty()
            val c = code?.trim().orEmpty()
            val tokenMode = t.isNotEmpty()
            val codeMode = e.isNotEmpty() && c.isNotEmpty()
            return (tokenMode && !codeMode) || (!tokenMode && codeMode)
        }
}
