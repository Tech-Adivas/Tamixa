package com.tamixa.infrastructure.voice

import com.tamixa.application.port.voice.VoiceReferenceStoragePort
import com.tamixa.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest

/**
 * Stores and retrieves parent reference audio samples in S3 for voice cloning.
 *
 * Path convention: voices/{parentId}/{fileName}
 */
@Component
@ConditionalOnExpression("@environment.getProperty('app.storage.type','s3').trim().toLowerCase() == 's3'")
class S3VoiceReferenceStorageAdapter(
    private val s3Client: S3Client,
    private val appProperties: AppProperties
) : VoiceReferenceStoragePort {

    private val log = LoggerFactory.getLogger(javaClass)
    private val bucket get() = appProperties.storage.effectiveS3Bucket

    override fun storeReferenceAudio(parentId: Long, fileName: String, bytes: ByteArray): String {
        val cleanedName = sanitizeFileName(fileName.ifBlank { "voice.wav" })
        val key = "voices/$parentId/$cleanedName"
        log.debug("S3 reference upload start parentId={} key={} bytes={}", parentId, key, bytes.size)
        return try {
            val request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("audio/mpeg")
                .build()
            s3Client.putObject(request, RequestBody.fromBytes(bytes))
            log.info("S3 reference upload success parentId={} key={} bytes={}", parentId, key, bytes.size)
            key
        } catch (e: Exception) {
            val msg = e.message ?: e.cause?.message ?: "Unknown error"
            log.error("S3 reference upload failed parentId={} key={} error={}", parentId, key, msg, e)
            throw IllegalStateException("Reference audio storage failed: ${msg.take(250)}", e)
        }
    }

    override fun getReferenceAudio(path: String): ByteArray? {
        if (path.isBlank() || !path.startsWith("voices/")) return null
        return try {
            val request = GetObjectRequest.builder().bucket(bucket).key(path).build()
            val response: ResponseInputStream<*> = s3Client.getObject(request)
            response.readAllBytes()
        } catch (e: Exception) {
            log.warn("S3 reference read failed path={}: {}", path, e.message)
            null
        }
    }

    override fun deleteReferenceAudio(path: String?) {
        if (path.isNullOrBlank() || !path.startsWith("voices/")) return
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(path).build())
            log.info("S3 reference delete success key={}", path)
        } catch (e: Exception) {
            log.warn("S3 reference delete failed path={}: {}", path, e.message)
        }
    }

    private fun sanitizeFileName(name: String): String =
        name.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(80).ifBlank { "voice.wav" }
}

