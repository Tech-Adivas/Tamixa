package com.araro.infrastructure.storage

import com.araro.application.port.FamilyVoiceStoragePort
import com.araro.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest

/**
 * Stores family recorded voice in private S3 path.
 * Path: families/{parentId}/stories/{storyId}/{language}.mp3
 * Enable with app.storage.type=s3 (default).
 */
@Component
@Primary
@ConditionalOnProperty(name = ["app.storage.type"], havingValue = "s3")
class S3FamilyVoiceStorageAdapter(
    private val s3Client: S3Client,
    private val appProperties: AppProperties
) : FamilyVoiceStoragePort {

    private val log = LoggerFactory.getLogger(javaClass)
    private val bucket get() = appProperties.storage.s3Bucket.ifBlank { "araro-audio" }

    override fun upload(storyId: Long, parentId: Long, language: String, audioBytes: ByteArray, contentType: String, fileExt: String): String {
        val key = "families/$parentId/stories/$storyId/${sanitizeLanguage(language)}.$fileExt"
        return try {
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(audioBytes.size.toLong())
                    .build(),
                RequestBody.fromBytes(audioBytes)
            )
            log.info("Stored family voice at {} ({} bytes)", key, audioBytes.size)
            key
        } catch (e: Exception) {
            log.error("Family voice upload failed storyId={} parentId={}: {}", storyId, parentId, e.message, e)
            throw e
        }
    }

    override fun delete(storagePath: String) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(storagePath).build())
            log.info("Deleted family voice at {}", storagePath)
        } catch (e: Exception) {
            log.warn("Failed to delete family voice at {}: {}", storagePath, e.message)
        }
    }

    override fun exists(storagePath: String): Boolean {
        return try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(storagePath).build())
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun sanitizeLanguage(lang: String) = lang.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(16).ifBlank { "default" }
}
