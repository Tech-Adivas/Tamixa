package com.araro.infrastructure.export

import com.araro.application.port.DataExportStoragePort
import com.araro.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest

/**
 * Stores GDPR/DPDP data export JSON in S3.
 * Path: exports/{parentId}/{jobId}/araro-data-export.json
 * Enable with app.storage.type=s3.
 */
@Component
@ConditionalOnProperty(name = ["app.storage.type"], havingValue = "s3")
class S3DataExportStorageAdapter(
    private val s3Client: S3Client,
    private val appProperties: AppProperties
) : DataExportStoragePort {

    private val log = LoggerFactory.getLogger(javaClass)
    private val bucket get() = appProperties.storage.s3Bucket.ifBlank { "araro-audio" }

    override fun uploadExport(parentId: Long, jobId: Long, jsonBytes: ByteArray): String {
        val key = "exports/$parentId/$jobId/araro-data-export.json"
        return try {
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("application/json")
                    .contentLength(jsonBytes.size.toLong())
                    .build(),
                RequestBody.fromBytes(jsonBytes)
            )
            log.info("Data export uploaded parentId={} jobId={} bytes={}", parentId, jobId, jsonBytes.size)
            key
        } catch (e: Exception) {
            log.error("Data export upload failed parentId={} jobId={}: {}", parentId, jobId, e.message, e)
            throw e
        }
    }
}
