package com.araro.repository

import com.araro.network.ConsentRecordDto
import com.araro.network.ExportJobDto
import com.araro.network.ListeningProgressDto
import com.araro.network.SettingsApi

class SettingsRepository(private val api: SettingsApi) {

    suspend fun getConsentRecords(): List<ConsentRecordDto> = api.getConsentRecords()

    suspend fun getDataExportJobs(): List<ExportJobDto> = api.getDataExportJobs()

    suspend fun requestDataExport(): ExportJobDto? = api.requestDataExport()

    suspend fun getListeningProgress(days: Int = 30): ListeningProgressDto? = api.getListeningProgress(days)
}
