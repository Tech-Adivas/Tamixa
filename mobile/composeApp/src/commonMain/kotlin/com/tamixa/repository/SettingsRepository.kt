package com.tamixa.repository

import com.tamixa.network.ConsentRecordDto
import com.tamixa.network.ExportJobDto
import com.tamixa.network.ListeningProgressDto
import com.tamixa.network.SettingsApi

class SettingsRepository(private val api: SettingsApi) {

    suspend fun getConsentRecords(): List<ConsentRecordDto> = api.getConsentRecords()

    suspend fun getDataExportJobs(): List<ExportJobDto> = api.getDataExportJobs()

    suspend fun requestDataExport(): ExportJobDto? = api.requestDataExport()

    suspend fun getListeningProgress(days: Int = 30): ListeningProgressDto? = api.getListeningProgress(days)

    suspend fun getListeningStreak(): Int? = api.getListeningStreak()
}
