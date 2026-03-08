package com.araro.domain

import kotlinx.serialization.Serializable

@Serializable
data class Child(
    val id: Long,
    val parentId: Long,
    val name: String,
    val dateOfBirth: String,
    val age: Int,
    val languagePreference: String?,
    val interests: String? = null,
    val createdAt: String,
    val favoriteColor: String? = null,
    val favoriteAnimal: String? = null,
    val characterTraits: String? = null,
    val avatarChoice: String? = null
)

@Serializable
data class CreateChildRequest(
    val name: String,
    val dateOfBirth: String,
    val languagePreference: String? = null,
    val interests: String? = null,
    val favoriteColor: String? = null,
    val favoriteAnimal: String? = null,
    val characterTraits: String? = null,
    val avatarChoice: String? = null,
    val childProfileConsent: Boolean = false
)

@Serializable
data class UpdateChildRequest(
    val languagePreference: String? = null,
    val interests: String? = null,
    val favoriteColor: String? = null,
    val favoriteAnimal: String? = null,
    val characterTraits: String? = null,
    val avatarChoice: String? = null
)
