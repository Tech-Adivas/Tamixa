package com.tamixa.infrastructure.cdn

import com.tamixa.application.port.ImageStoragePort
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
 * Stores cover images in S3. Used when app.storage.type=s3 (default).
 * Paths: covers/{storyId}.png, curated_covers/{curatedStoryId}.png
 */
@Component
@Primary
@ConditionalOnProperty(name = ["app.storage.type"], havingValue = "s3")
class S3ImageStorageAdapter(
    private val s3Client: S3Client,
    private val appProperties: AppProperties
) : ImageStoragePort {

    private val log = LoggerFactory.getLogger(javaClass)
    private val bucket get() = appProperties.storage.effectiveS3Bucket

    override fun storeCoverImage(storyId: Long, imageBytes: ByteArray): String? {
        val key = "covers/$storyId.png"
        return try {
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("image/png")
                    .build(),
                RequestBody.fromBytes(imageBytes)
            )
            log.info("Stored cover image for story {} at {}", storyId, key)
            key
        } catch (e: Exception) {
            log.warn("Failed to store cover image for story {}: {}", storyId, e.message)
            null
        }
    }

    override fun storeCuratedCoverImage(curatedStoryId: Long, imageBytes: ByteArray): String? {
        val key = "curated_covers/$curatedStoryId.png"
        log.info("Storing curated cover for story {} ({} bytes) to bucket={} key={}", curatedStoryId, imageBytes.size, bucket.take(50), key)
        return try {
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("image/png")
                    .build(),
                RequestBody.fromBytes(imageBytes)
            )
            log.info("Stored curated cover image for story {} at {}", curatedStoryId, key)
            key
        } catch (e: Exception) {
            log.warn("Failed to store curated cover image for story {}: {} cause={}", curatedStoryId, e.message, e.cause?.message, e)
            null
        }
    }

    override fun deleteCuratedCoverImage(storageKey: String?) {
        if (storageKey.isNullOrBlank()) return
        val key = storageKey.trim()
        if (!key.startsWith("curated_covers/")) return
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build())
            log.info("Deleted curated cover image from S3: {}", key)
        } catch (e: Exception) {
            log.warn("Failed to delete curated cover image {}: {}", key, e.message)
        }
    }

    override fun deleteCoverImage(storageKey: String?) {
        if (storageKey.isNullOrBlank()) return
        val key = storageKey.trim()
        if (!key.startsWith("covers/")) return
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build())
            log.info("Deleted generated cover image from S3: {}", key)
        } catch (e: Exception) {
            log.warn("Failed to delete generated cover image {}: {}", key, e.message)
        }
    }

    override fun storeInteractiveSegmentImage(libraryStoryId: Long, segmentId: String, imageBytes: ByteArray): String? {
        val safeSegment = segmentId.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(80).ifBlank { "segment" }
        val key = "interactive_images/$libraryStoryId/$safeSegment.png"
        return try {
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("image/png")
                    .build(),
                RequestBody.fromBytes(imageBytes)
            )
            log.info("Stored interactive question image for story {} segment {} at {}", libraryStoryId, segmentId, key)
            key
        } catch (e: Exception) {
            log.warn("Failed to store interactive question image for story {} segment {}: {}", libraryStoryId, segmentId, e.message)
            null
        }
    }

    override fun storeStoryVisualImage(libraryStoryId: Long, name: String, jpegBytes: ByteArray): String? {
        val safeName = name.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(80).ifBlank { "image" }
        val key = "curated_covers/visuals/$libraryStoryId/$safeName.jpg"
        return try {
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("image/jpeg")
                    .build(),
                RequestBody.fromBytes(jpegBytes)
            )
            log.info("Stored story visual image for story {} at {} bytes={}", libraryStoryId, key, jpegBytes.size)
            key
        } catch (e: Exception) {
            log.warn("Failed to store story visual image for story {} name {}: {}", libraryStoryId, name, e.message)
            null
        }
    }
}
