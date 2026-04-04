package com.tamixa.network

import com.tamixa.util.TamixaLog
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.CancellationException

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
        client.get("${ApiConfig.API_VERSION}/consent").bodyIfSuccess() ?: emptyList()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        TamixaLog.w("SettingsApi", "getConsentRecords failed", e)
        emptyList()
    }

    suspend fun getDataExportJobs(): List<ExportJobDto> = try {
        client.get("${ApiConfig.API_VERSION}/data-export").bodyIfSuccess() ?: emptyList()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        TamixaLog.w("SettingsApi", "getDataExportJobs failed", e)
        emptyList()
    }

    suspend fun requestDataExport(): ExportJobDto? = try {
        client.post("${ApiConfig.API_VERSION}/data-export/request").bodyIfSuccess()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        TamixaLog.w("SettingsApi", "requestDataExport failed", e)
        null
    }

    suspend fun getListeningProgress(days: Int = 30): ListeningProgressDto? = try {
        client.get("${ApiConfig.API_VERSION}/listening-progress") {
            parameter("days", days)
        }.bodyIfSuccess()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        TamixaLog.w("SettingsApi", "getListeningProgress failed", e)
        null
    }

    /** Consecutive days with at least one story play. For Dashboard streak. */
    suspend fun getListeningStreak(): Int? = try {
        client.get("${ApiConfig.API_VERSION}/listening-progress/streak").bodyIfSuccess<ListeningStreakDto>()?.streakDays
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        TamixaLog.w("SettingsApi", "getListeningStreak failed", e)
        null
    }
}

@kotlinx.serialization.Serializable
data class ListeningStreakDto(val streakDays: Int)
