package com.tamixa.network

import io.ktor.client.*
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
        try {
            client.get("${ApiConfig.API_VERSION}/achievements/children/$childId").bodyIfSuccess() ?: emptyList()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }

    suspend fun getDefinitions(): List<AchievementDefinitionDto> =
        try {
            client.get("${ApiConfig.API_VERSION}/achievements/definitions").bodyIfSuccess() ?: emptyList()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }

    suspend fun getMe(): List<AchievementDto> =
        try {
            client.get("${ApiConfig.API_VERSION}/achievements/me").bodyIfSuccess() ?: emptyList()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
}

@kotlinx.serialization.Serializable
data class AchievementDefinitionDto(
    val type: String,
    val name: String,
    val description: String,
    val requiredCompletions: Long
)
