package com.araro.infrastructure.storage

import com.araro.application.port.StoryAvatarVideoStoragePort
import com.araro.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest

@Component
@ConditionalOnProperty(name = ["app.storage.type"], havingValue = "s3")
class S3StoryAvatarVideoStorageAdapter(
    private val s3Client: S3Client,
    private val appProperties: AppProperties
) : StoryAvatarVideoStoragePort {

    private val log = LoggerFactory.getLogger(javaClass)
    private val bucket get() = appProperties.storage.s3Bucket.ifBlank { "araro-audio" }

    override fun upload(storagePath: String, videoBytes: ByteArray) {
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(storagePath)
                .contentType("video/mp4")
                .contentLength(videoBytes.size.toLong())
                .build(),
            RequestBody.fromBytes(videoBytes)
        )
        log.info("Stored avatar video at {} ({} bytes)", storagePath, videoBytes.size)
    }

    override fun delete(storagePath: String) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(storagePath).build())
            log.info("Deleted avatar video at {}", storagePath)
        } catch (e: Exception) {
            log.warn("Failed to delete avatar video at {}: {}", storagePath, e.message)
        }
    }
}
