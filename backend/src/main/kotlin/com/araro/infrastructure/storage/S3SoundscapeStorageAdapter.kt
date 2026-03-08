package com.araro.infrastructure.storage

import com.araro.application.port.SoundscapeStoragePort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.io.ByteArrayResource
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest

/**
 * S3 storage adapter for soundscape audio files.
 * Path: soundscapes/{soundscapeId}.mp3
 */
@Component
@ConditionalOnProperty(name = ["app.storage.type"], havingValue = "s3")
class S3SoundscapeStorageAdapter(
    private val s3Client: S3Client,
    @Value("\${app.storage.s3-bucket:araro-audio}") private val bucket: String,
    @Value("\${app.storage.s3-region:us-east-1}") private val region: String
) : SoundscapeStoragePort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun upload(soundscapeId: Long, audioBytes: ByteArray, contentType: String): String {
        val key = "soundscapes/${soundscapeId}.mp3"
        val request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .build()

        s3Client.putObject(request, RequestBody.fromBytes(audioBytes))
        val url = "https://$bucket.s3.$region.amazonaws.com/$key"
        log.info("Soundscape uploaded to S3: {}", url)
        return url
    }

    override fun delete(storagePath: String) {
        // Extract key from URL if needed
        val key = storagePath.removePrefix("https://$bucket.s3.$region.amazonaws.com/")
        s3Client.deleteObject { it.bucket(bucket).key(key) }
        log.info("Soundscape deleted from S3: {}", key)
    }

    override fun exists(storagePath: String): Boolean {
        val key = storagePath.removePrefix("https://$bucket.s3.$region.amazonaws.com/")
        return s3Client.headObject { it.bucket(bucket).key(key) } != null
    }
}
