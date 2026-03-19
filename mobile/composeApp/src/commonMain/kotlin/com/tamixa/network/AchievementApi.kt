package com.tamixa.network

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*

@kotlinx.serialization.Serializable
data class AchievementDto(
    val type: String,
    val name: String,
    val description: String,
    val requiredCompletions: Long,
    val earned: Boolean,
    val earnedAt: String? = null
)

class AchievementApi(private val client: HttpClient) {

    suspend fun getByChild(childId: Long): List<AchievementDto> =
        client.get("${ApiConfig.API_VERSION}/achievements/children/$childId").body()

    suspend fun getDefinitions(): List<AchievementDefinitionDto> =
        client.get("${ApiConfig.API_VERSION}/achievements/definitions").body()

    suspend fun getMe(): List<AchievementDto> =
        client.get("${ApiConfig.API_VERSION}/achievements/me").body()
}

@kotlinx.serialization.Serializable
data class AchievementDefinitionDto(
    val type: String,
    val name: String,
    val description: String,
    val requiredCompletions: Long
)
