package com.tamixa.application.profile

data class ProfileResponse(
    val parent: ProfileParentDto,
    val children: List<ProfileChildDto> = emptyList(),
    val subscriptionPlan: String? = null
)

data class ProfileParentDto(
    val id: Long,
    val email: String,
    val nickname: String? = null,
    val displayName: String? = null
)

data class ProfileChildDto(
    val id: Long,
    val name: String,
    val dateOfBirth: String,
    val languagePreference: String?
)
