package com.araro.api.child.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Past
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class CreateChildRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(min = 1, max = 255)
    val name: String,

    @field:NotNull(message = "Date of birth is required")
    @field:Past(message = "Date of birth must be in the past")
    val dateOfBirth: LocalDate,

    @field:Size(max = 10, message = "Language preference must be at most 10 characters")
    val languagePreference: String? = null,

    @field:Size(max = 500, message = "Interests must be at most 500 characters")
    val interests: String? = null,

    @field:Size(max = 50)
    val favoriteColor: String? = null,

    @field:Size(max = 100)
    val favoriteAnimal: String? = null,

    @field:Size(max = 500, message = "Character traits must be at most 500 characters")
    val characterTraits: String? = null,

    @field:Size(max = 50)
    val avatarChoice: String? = null,

    /** Required for COPPA/DPDP/GDPR: parental consent to collect child data */
    val childProfileConsent: Boolean = false
)
