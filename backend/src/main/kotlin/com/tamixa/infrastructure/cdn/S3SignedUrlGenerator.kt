package com.tamixa.infrastructure.cdn

import com.tamixa.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.net.URL
import java.time.Duration

/**
 * Generates presigned S3 URLs for private audio streaming.
 * 10-minute expiry per security requirements. Never returns raw S3 path.
 */
@Component
@ConditionalOnProperty(name = ["app.storage.type"], havingValue = "s3")
class S3SignedUrlGenerator(
    private val s3Presigner: S3Presigner,
    private val appProperties: AppProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val bucket get() = appProperties.storage.effectiveS3Bucket

    fun signUrl(objectKey: String, expiryMinutes: Long = 10): URL? {
        log.debug("🔍 signUrl called: key={}, expiryMin={}, bucket={}", objectKey, expiryMinutes, bucket)
        return try {
            // For Access Point ARNs, use the ARN directly as the bucket parameter
            // S3Presigner will handle the ARN correctly
            val getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build()
            val presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expiryMinutes))
                .getObjectRequest(getObjectRequest)
                .build()
            val presigned = s3Presigner.presignGetObject(presignRequest)
            val url = presigned.url()
            log.info("✅ S3 presigned URL generated: key={}, expiryMin={}, url={}", objectKey, expiryMinutes, url)
            url
        } catch (e: Exception) {
            log.error("❌ S3 presign failed: key={}, bucket={}, error={}", objectKey, bucket, e.message, e)
            null
        }
    }
}
