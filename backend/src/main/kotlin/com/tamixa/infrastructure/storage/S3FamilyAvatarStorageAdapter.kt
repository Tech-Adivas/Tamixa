package com.tamixa.infrastructure.storage

import com.tamixa.application.port.FamilyAvatarStoragePort
import com.tamixa.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest

/**
 * Stores parent avatar in S3.
 * Path: avatars/{parentId}/avatar.jpg or avatar.png
 */
@Component
@Primary
@ConditionalOnProperty(name = ["app.storage.type"], havingValue = "s3")
class S3FamilyAvatarStorageAdapter(
    private val s3Client: S3Client,
    private val appProperties: AppProperties
) : FamilyAvatarStoragePort {

    private val log = LoggerFactory.getLogger(javaClass)
    private val bucket get() = appProperties.storage.effectiveS3Bucket

    override fun upload(parentId: Long, imageBytes: ByteArray, contentType: String): String {
        val ext = if (contentType.contains("png")) "png" else "jpg"
        val key = "avatars/$parentId/avatar.$ext"
        return try {
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(imageBytes.size.toLong())
                    .build(),
                RequestBody.fromBytes(imageBytes)
            )
            log.info("Stored avatar at {} ({} bytes)", key, imageBytes.size)
            key
        } catch (e: Exception) {
            log.error("Avatar upload failed parentId={}: {}", parentId, e.message, e)
            throw e
        }
    }

    override fun delete(storagePath: String) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(storagePath).build())
            log.info("Deleted avatar at {}", storagePath)
        } catch (e: Exception) {
            log.warn("Failed to delete avatar at {}: {}", storagePath, e.message)
        }
    }
}
