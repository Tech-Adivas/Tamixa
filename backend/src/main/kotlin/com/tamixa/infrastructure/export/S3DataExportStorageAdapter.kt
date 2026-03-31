package com.tamixa.infrastructure.export

import com.tamixa.application.port.DataExportStoragePort
import com.tamixa.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.io.InputStream
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest

/**
 * Stores GDPR/DPDP data export JSON in S3.
 * Path: exports/{parentId}/{jobId}/tamixa-data-export.json
 * Enable with app.storage.type=s3.
 */
@Component
@ConditionalOnProperty(name = ["app.storage.type"], havingValue = "s3")
class S3DataExportStorageAdapter(
    private val s3Client: S3Client,
    private val appProperties: AppProperties
) : DataExportStoragePort {

    private val log = LoggerFactory.getLogger(javaClass)
    private val bucket get() = appProperties.storage.effectiveS3Bucket

    override fun uploadExportStream(parentId: Long, jobId: Long, inputStream: InputStream, contentLength: Long): String {
        val key = "exports/$parentId/$jobId/tamixa-data-export.json"
        return try {
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("application/json")
                    .contentLength(contentLength)
                    .build(),
                RequestBody.fromInputStream(inputStream, contentLength)
            )
            log.info("Data export uploaded parentId={} jobId={} bytes={}", parentId, jobId, contentLength)
            key
        } catch (e: Exception) {
            log.error("Data export upload failed parentId={} jobId={}: {}", parentId, jobId, e.message, e)
            throw e
        }
    }
}
