package com.araro.api.child.dto

import java.time.Instant
import java.time.LocalDate

data class ChildResponse(
    val id: Long,
    val parentId: Long,
    val name: String,
    val dateOfBirth: LocalDate,
    val age: Int,
    val languagePreference: String?,
    val interests: String? = null,
    val createdAt: Instant,
    val favoriteColor: String? = null,
    val favoriteAnimal: String? = null,
    val characterTraits: String? = null,
    val avatarChoice: String? = null
)
