package com.araro.infrastructure.narration

import com.araro.application.narration.AudioStorageService
import com.araro.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest

/**
 * Stores narration audio in S3.
 * Path: stories/{storyId}/{language}/{voiceProfileSlug}.mp3
 * Enable with app.storage.type=s3.
 */
@Component
@ConditionalOnProperty(name = ["app.storage.type"], havingValue = "s3")
class S3NarrationAudioStorageAdapter(
    private val s3Client: S3Client,
    private val appProperties: AppProperties
) : AudioStorageService {

    private val log = LoggerFactory.getLogger(javaClass)
    private val bucket get() = appProperties.storage.s3Bucket.ifBlank { "araro-audio" }

    override fun uploadNarrationAudio(
        storyId: Long,
        language: String,
        voiceProfile: String,
        mp3Bytes: ByteArray
    ): String {
        val slug = if (voiceProfile == "default") "v1" else voiceProfileSlug(voiceProfile)
        val key = "stories/$storyId/${sanitizeLanguage(language)}/$slug.mp3"
        log.debug("S3 upload start storyId={} key={} bytes={}", storyId, key, mp3Bytes.size)
        return try {
            val request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("audio/mpeg")
                .build()
            s3Client.putObject(request, RequestBody.fromBytes(mp3Bytes))
            log.info("S3 upload success storyId={} key={} bytes={}", storyId, key, mp3Bytes.size)
            key
        } catch (e: Exception) {
            val msg = e.message ?: e.cause?.message ?: "Unknown error"
            log.error("S3 upload failed storyId={} key={} error={}", storyId, key, msg, e)
            throw IllegalStateException("Audio storage failed: ${msg.take(250)}", e)
        }
    }

    private fun voiceProfileSlug(profile: String): String =
        profile.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(50).ifBlank { "v1" }

    private fun sanitizeLanguage(lang: String): String =
        lang.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(16).ifBlank { "default" }
}
