package com.tamixa.infrastructure.storage

import com.tamixa.application.port.ProxyStoragePort
import com.tamixa.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

@Component
@ConditionalOnProperty(name = ["app.storage.type"], havingValue = "s3")
class S3ProxyStorageAdapter(
    private val s3Client: S3Client,
    private val appProperties: AppProperties
) : ProxyStoragePort {

    private val log = LoggerFactory.getLogger(javaClass)
    private val bucket get() = appProperties.storage.effectiveS3Bucket

    override fun getObject(key: String): ByteArray? = try {
        val request = GetObjectRequest.builder().bucket(bucket).key(key).build()
        val response: ResponseInputStream<*> = s3Client.getObject(request)
        response.readAllBytes()
    } catch (e: NoSuchKeyException) {
        log.debug("S3 object not found: key={}", key)
        null
    } catch (e: Exception) {
        log.warn("Failed to get S3 object key={}: {}", key, e.message)
        null
    }

    override fun getContentLength(key: String): Long? = try {
        val request = HeadObjectRequest.builder().bucket(bucket).key(key).build()
        s3Client.headObject(request).contentLength()
    } catch (e: NoSuchKeyException) {
        null
    } catch (e: Exception) {
        log.warn("Failed to head S3 object key={}: {}", key, e.message)
        null
    }

    override fun getObjectRange(key: String, start: Long, end: Long): ByteArray? = try {
        val request = GetObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .range("bytes=$start-$end")
            .build()
        val response: ResponseInputStream<*> = s3Client.getObject(request)
        response.readAllBytes()
    } catch (e: NoSuchKeyException) {
        null
    } catch (e: Exception) {
        log.warn("Failed to get S3 object range key={}: {}", key, e.message)
        null
    }
}
