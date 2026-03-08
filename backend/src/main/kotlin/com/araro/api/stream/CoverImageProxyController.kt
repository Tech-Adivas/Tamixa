package com.araro.api.stream

import com.araro.api.ApiVersion
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

/**
 * Proxies GET /api/v1/covers/{path} to S3.
 * Used when presigned S3 URLs fail (CORS, access points). Returns proxy URLs instead.
 */
@RestController
@RequestMapping("${ApiVersion.V1}/covers")
@ConditionalOnProperty(name = ["app.storage.type"], havingValue = "s3")
class CoverImageProxyController(
    private val s3Client: S3Client,
    private val appProperties: AppProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val bucket get() = appProperties.storage.s3Bucket.ifBlank { "araro-audio" }

    @GetMapping("/**")
    fun streamCover(request: HttpServletRequest): ResponseEntity<Any> {
        val path = request.requestURI
            .substringAfter("/covers/")
            .takeIf { it.isNotBlank() }
            ?: return ResponseEntity.badRequest().build()
        val key = path
        if (!key.startsWith("covers/") && !key.startsWith("curated_covers/") && !key.startsWith("curated_cover_videos/")) {
            log.warn("Rejected invalid cover path: {}", key)
            return ResponseEntity.badRequest().build()
        }
        return try {
            val getRequest = GetObjectRequest.builder().bucket(bucket).key(key).build()
            val response: ResponseInputStream<*> = s3Client.getObject(getRequest)
            val bytes = response.readAllBytes()
            val contentType = when {
                key.endsWith(".gif") -> MediaType.parseMediaType("image/gif")
                key.endsWith(".mp4") -> MediaType.parseMediaType("video/mp4")
                key.endsWith(".png") -> MediaType.IMAGE_PNG
                key.endsWith(".jpg") || key.endsWith(".jpeg") -> MediaType.IMAGE_JPEG
                key.endsWith(".webp") -> MediaType.parseMediaType("image/webp")
                else -> MediaType.IMAGE_PNG
            }
            ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_LENGTH, bytes.size.toString())
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(bytes)
        } catch (e: NoSuchKeyException) {
            log.debug("Cover not found in S3: key={}", key)
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            log.warn("Failed to stream cover key={}: {}", key, e.message)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}
