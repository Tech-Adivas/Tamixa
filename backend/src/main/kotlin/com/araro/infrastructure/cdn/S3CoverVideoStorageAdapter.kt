package com.araro.infrastructure.cdn

import com.araro.application.port.CoverVideoStoragePort
import com.araro.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest

/**
 * Stores cover animations (GIF) in S3. Paths: curated_cover_videos/{curatedStoryId}.gif
 */
@Component
@ConditionalOnProperty(name = ["app.storage.type"], havingValue = "s3")
class S3CoverVideoStorageAdapter(
    private val s3Client: S3Client,
    private val appProperties: AppProperties
) : CoverVideoStoragePort {

    private val log = LoggerFactory.getLogger(javaClass)
    private val bucket get() = appProperties.storage.s3Bucket.ifBlank { "araro-audio" }

    override fun storeCuratedCoverVideo(curatedStoryId: Long, animationBytes: ByteArray): String? {
        val key = "curated_cover_videos/$curatedStoryId.gif"
        return try {
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("image/gif")
                    .build(),
                RequestBody.fromBytes(animationBytes)
            )
            log.info("Stored curated cover GIF for story {} at {} ({} bytes)", curatedStoryId, key, animationBytes.size)
            key
        } catch (e: Exception) {
            log.warn("Failed to store curated cover GIF for story {}: {}", curatedStoryId, e.message)
            null
        }
    }

    override fun deleteCuratedCoverVideo(storageKey: String?) {
        if (storageKey.isNullOrBlank()) return
        val key = storageKey.trim()
        if (!key.startsWith("curated_cover_videos/")) return
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build())
            log.info("Deleted curated cover video from S3: {}", key)
        } catch (e: Exception) {
            log.warn("Failed to delete curated cover video {}: {}", key, e.message)
        }
    }
}
