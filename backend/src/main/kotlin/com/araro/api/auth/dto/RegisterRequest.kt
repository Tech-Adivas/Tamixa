package com.araro.api.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    val password: String,

    /** Required for DPDP/COPPA/GDPR: explicit consent to Terms of Service */
    val acceptedTerms: Boolean = false,
    /** Required for DPDP/COPPA/GDPR: explicit consent to Privacy Policy */
    val acceptedPrivacy: Boolean = false,
    /** Required for COPPA: attestation that the user is the parent/guardian and at least 18 years old */
    val acceptedParentalAttestation: Boolean = false
)
