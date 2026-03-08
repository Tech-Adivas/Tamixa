package com.araro.application.export

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant

/**
 * GDPR/DPDP data export payload. Excludes sensitive fields (password hash).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class DataExportPayload(
    val exportedAt: String,
    val parent: ParentExport,
    val children: List<ChildExport>,
    val stories: List<StoryExport>,
    val favorites: List<FavoriteExport>,
    val consents: List<ConsentExport>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ParentExport(
    val id: Long,
    val email: String,
    val phone: String?,
    val createdAt: String,
    val role: String
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ChildExport(
    val id: Long,
    val name: String,
    val dateOfBirth: String,
    val languagePreference: String?,
    val interests: String?,
    val createdAt: String,
    val favoriteColor: String?,
    val favoriteAnimal: String?,
    val characterTraits: String?,
    val avatarChoice: String?
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class StoryExport(
    val id: Long,
    val theme: String,
    val language: String,
    val age: Int,
    val childName: String,
    val title: String?,
    val moral: String?,
    val wordCount: Int,
    val readingTimeMinutes: Double,
    val status: String,
    val createdAt: String
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FavoriteExport(
    val storyId: Long,
    val storySource: String
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ConsentExport(
    val consentType: String,
    val version: Int,
    val grantedAt: String
)
