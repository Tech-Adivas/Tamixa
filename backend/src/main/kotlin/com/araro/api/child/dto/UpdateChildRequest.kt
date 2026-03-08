package com.araro.api.child.dto

import jakarta.validation.constraints.Size

data class UpdateChildRequest(
    @field:Size(max = 10)
    val languagePreference: String? = null,

    @field:Size(max = 500)
    val interests: String? = null,

    @field:Size(max = 50)
    val favoriteColor: String? = null,

    @field:Size(max = 100)
    val favoriteAnimal: String? = null,

    @field:Size(max = 500)
    val characterTraits: String? = null,

    @field:Size(max = 50)
    val avatarChoice: String? = null
)
