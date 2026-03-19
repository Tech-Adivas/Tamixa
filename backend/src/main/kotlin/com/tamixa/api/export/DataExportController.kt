package com.tamixa.api.export

import com.tamixa.api.ApiVersion
import com.tamixa.application.export.DataExportService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${ApiVersion.V1}/data-export")
@PreAuthorize("hasRole('PARENT')")
class DataExportController(
    private val dataExportService: DataExportService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/request")
    fun request(): ResponseEntity<ExportJobResponse> {
        val email = SecurityContextHolder.getContext().authentication?.name ?: throw IllegalStateException("Not authenticated")
        log.info("Data export requested")
        val job = dataExportService.requestExport(email)
        return ResponseEntity.ok(ExportJobResponse(job.id, job.status, job.requestedAt.toString(), job.downloadUrl))
    }

    @GetMapping
    fun list(): ResponseEntity<List<ExportJobResponse>> {
        val email = SecurityContextHolder.getContext().authentication?.name ?: throw IllegalStateException("Not authenticated")
        log.debug("Data export list requested")
        val jobs = dataExportService.listJobs(email)
        return ResponseEntity.ok(jobs.map { ExportJobResponse(it.id, it.status, it.requestedAt.toString(), it.downloadUrl) })
    }
}

data class ExportJobResponse(val id: Long, val status: String, val requestedAt: String, val downloadUrl: String?)