package com.tamixa.api.stream

import com.tamixa.api.ApiVersion
import com.tamixa.application.port.ProxyStoragePort
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
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Proxies GET /api/v1/covers/{path} to backing storage.
 * Used when presigned S3 URLs fail (CORS, access points). Returns proxy URLs instead.
 */
@RestController
@RequestMapping("${ApiVersion.V1}/covers")
@ConditionalOnProperty(name = ["app.storage.type"], havingValue = "s3")
class CoverImageProxyController(
    private val proxyStorage: ProxyStoragePort,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/**")
    fun streamCover(request: HttpServletRequest): ResponseEntity<Any> {
        val path = request.requestURI
            .substringAfter("/covers/")
            .takeIf { it.isNotBlank() }
            ?: return jsonError(HttpStatus.BAD_REQUEST, "Missing path")
        val key = path
        if (!key.startsWith("covers/") && !key.startsWith("curated_covers/") && !key.startsWith("curated_cover_videos/") && !key.startsWith("generated_cover_videos/")) {
            log.warn("Rejected invalid cover path: {}", key)
            return jsonError(HttpStatus.BAD_REQUEST, "Invalid path")
        }
        val bytes = proxyStorage.getObject(key)
            ?: return jsonError(HttpStatus.NOT_FOUND, "Cover not found")
        val contentType = when {
                key.endsWith(".gif") -> MediaType.parseMediaType("image/gif")
                key.endsWith(".mp4") -> MediaType.parseMediaType("video/mp4")
                key.endsWith(".png") -> MediaType.IMAGE_PNG
                key.endsWith(".jpg") || key.endsWith(".jpeg") -> MediaType.IMAGE_JPEG
                key.endsWith(".webp") -> MediaType.parseMediaType("image/webp")
                else -> MediaType.IMAGE_PNG
            }
        return ResponseEntity.ok()
            .contentType(contentType)
            .header(HttpHeaders.CONTENT_LENGTH, bytes.size.toString())
            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
            .body(bytes)
    }

    private fun jsonError(status: HttpStatus, message: String): ResponseEntity<Any> {
        val body = mapOf("message" to message, "status" to status.value())
        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectMapper.writeValueAsString(body))
    }
}
