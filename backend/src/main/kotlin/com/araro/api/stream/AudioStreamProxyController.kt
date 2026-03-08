package com.araro.api.stream

import com.araro.infrastructure.config.AppProperties
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

// Proxies GET /audio/stories/... to S3 when presigned URLs fall back to direct URLs
@RestController
@RequestMapping("/audio")
@ConditionalOnProperty(name = ["app.storage.type"], havingValue = "s3")
class AudioStreamProxyController(
    private val s3Client: S3Client,
    private val appProperties: AppProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val bucket get() = appProperties.storage.s3Bucket.ifBlank { "araro-audio" }

    @GetMapping("/**")
    fun streamAudio(request: HttpServletRequest): ResponseEntity<Any> {
        val key = request.requestURI.removePrefix("/audio").removePrefix("/").takeIf { it.isNotBlank() }
            ?: return ResponseEntity.badRequest().build()
        if (!key.startsWith("stories/")) {
            log.warn("Rejected invalid audio path: {}", key)
            return ResponseEntity.badRequest().build()
        }
        return try {
            log.info("Audio proxy: key={}", key)
            val getRequest = GetObjectRequest.builder().bucket(bucket).key(key).build()
            val response: ResponseInputStream<*> = s3Client.getObject(getRequest)
            val bytes = response.readAllBytes()
            log.info("Audio proxy: served key={} size={}", key, bytes.size)
            ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header(HttpHeaders.CONTENT_LENGTH, bytes.size.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(bytes)
        } catch (e: NoSuchKeyException) {
            log.warn("Audio not found in S3: key={} (ExoPlayer will get 404 and fail)", key)
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            log.warn("Failed to stream audio key={}: {}", key, e.message)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}
