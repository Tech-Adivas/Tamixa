package com.tamixa.application.export

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Processes pending GDPR/DPDP data export jobs every 2 minutes.
 * Requires app.storage.type=s3. When storage is not configured, jobs remain pending.
 */
@Component
class DataExportProcessor(
    private val dataExportService: DataExportService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${app.export.process-cron:0 */2 * * * *}")
    fun processPendingExports() {
        var processed = 0
        while (dataExportService.processNextPendingJob()) {
            processed++
            if (processed >= 5) break // cap per run to avoid long delays
        }
        if (processed > 0) {
            log.info("DataExportProcessor: processed {} export job(s)", processed)
        }
    }
}
