package com.tamixa.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

@kotlinx.serialization.Serializable
data class ConsentRecordDto(
    val consentType: String,
    val version: Int,
    val grantedAt: String
)

@kotlinx.serialization.Serializable
data class ExportJobDto(
    val id: Long,
    val status: String,
    val requestedAt: String,
    val downloadUrl: String?
)

@kotlinx.serialization.Serializable
data class ListeningProgressDto(
    val periodDays: Int,
    val storiesStarted: Long,
    val storiesCompleted: Long,
    val completionRate: Double
)

class SettingsApi(private val client: HttpClient) {

    suspend fun getConsentRecords(): List<ConsentRecordDto> = try {
        client.get("${ApiConfig.API_VERSION}/consent").body()
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun getDataExportJobs(): List<ExportJobDto> = try {
        client.get("${ApiConfig.API_VERSION}/data-export").body()
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun requestDataExport(): ExportJobDto? = try {
        client.post("${ApiConfig.API_VERSION}/data-export/request").body<ExportJobDto>()
    } catch (_: Exception) {
        null
    }

    suspend fun getListeningProgress(days: Int = 30): ListeningProgressDto? = try {
        client.get("${ApiConfig.API_VERSION}/listening-progress") {
            parameter("days", days)
        }.body<ListeningProgressDto>()
    } catch (_: Exception) {
        null
    }

    /** Consecutive days with at least one story play. For Dashboard streak. */
    suspend fun getListeningStreak(): Int? = try {
        client.get("${ApiConfig.API_VERSION}/listening-progress/streak").body<ListeningStreakDto>()?.streakDays
    } catch (_: Exception) {
        null
    }
}

@kotlinx.serialization.Serializable
data class ListeningStreakDto(val streakDays: Int)
