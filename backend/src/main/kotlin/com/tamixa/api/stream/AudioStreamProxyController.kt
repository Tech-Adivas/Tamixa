package com.tamixa.api.stream

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

// Proxies GET /audio/stories/... to backing storage when presigned URLs fall back to direct URLs.
// Supports HTTP Range requests (206 Partial Content) so iOS AVPlayer does not fail with -12939.
// Content-Type is derived from file extension so multiple audio formats (MP3, M4A, AAC, OGG, WAV) are supported.
@RestController
@RequestMapping("/audio")
@ConditionalOnProperty(name = ["app.storage.type"], havingValue = "s3")
class AudioStreamProxyController(
    private val proxyStorage: ProxyStoragePort,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Map file extension to media type for correct playback and Range requests. */
    private fun contentTypeForKey(key: String): MediaType {
        val ext = key.substringAfterLast('.').lowercase().take(8)
        return when (ext) {
            "mp3" -> MediaType.parseMediaType("audio/mpeg")
            "m4a", "mp4" -> MediaType.parseMediaType("audio/mp4")
            "aac" -> MediaType.parseMediaType("audio/aac")
            "ogg" -> MediaType.parseMediaType("audio/ogg")
            "wav" -> MediaType.parseMediaType("audio/wav")
            "weba", "webm" -> MediaType.parseMediaType("audio/webm")
            else -> MediaType.parseMediaType("audio/mpeg") // default for unknown
        }
    }

    @GetMapping("/**")
    fun streamAudio(request: HttpServletRequest): ResponseEntity<Any> {
        val key = request.requestURI.removePrefix("/audio").removePrefix("/").takeIf { it.isNotBlank() }
            ?: return jsonError(HttpStatus.BAD_REQUEST, "Missing path")
        if (!key.startsWith("stories/")) {
            log.warn("Rejected invalid audio path: {}", key)
            return jsonError(HttpStatus.BAD_REQUEST, "Invalid path")
        }
        val rangeHeader = request.getHeader(HttpHeaders.RANGE)?.trim()?.takeIf { it.lowercase().startsWith("bytes=") }
        return if (rangeHeader != null) {
            streamAudioRange(key, rangeHeader)
        } else {
            streamAudioFull(key)
        }
    }

    private fun jsonError(status: HttpStatus, message: String): ResponseEntity<Any> {
        val body = mapOf("message" to message, "status" to status.value())
        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectMapper.writeValueAsString(body))
    }

    private fun streamAudioFull(key: String): ResponseEntity<Any> {
        log.info("Audio proxy: key={}", key)
        val bytes = proxyStorage.getObject(key)
            ?: return jsonError(HttpStatus.NOT_FOUND, "Audio not found")
        log.info("Audio proxy: served key={} size={}", key, bytes.size)
        return ResponseEntity.ok()
            .contentType(contentTypeForKey(key))
            .header(HttpHeaders.CONTENT_LENGTH, bytes.size.toString())
            .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
            .header(HttpHeaders.PRAGMA, "no-cache")
            .body(bytes)
    }

    private fun streamAudioRange(key: String, rangeHeader: String): ResponseEntity<Any> {
        val totalLength = proxyStorage.getContentLength(key)
            ?: return jsonError(HttpStatus.NOT_FOUND, "Audio not found")
        val rangeValue = rangeHeader.removePrefix("bytes=").trim()
        val (start, end) = when {
            rangeValue.contains("-") -> {
                val parts = rangeValue.split("-", limit = 2)
                val s = parts[0].toLongOrNull() ?: 0L
                val e = parts.getOrNull(1)?.takeIf { it.isNotBlank() }?.toLongOrNull() ?: (totalLength - 1).coerceAtLeast(s)
                s to e.coerceAtMost(totalLength - 1).coerceAtLeast(s)
            }
            else -> 0L to (totalLength - 1).coerceAtLeast(0L)
        }
        val length = (end - start + 1).coerceIn(0, totalLength)
        val bytes = proxyStorage.getObjectRange(key, start, end)
            ?: return jsonError(HttpStatus.INTERNAL_SERVER_ERROR, "Stream failed")
        log.debug("Audio proxy: range key={} bytes={}-{}/{}", key, start, end, totalLength)
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
            .contentType(contentTypeForKey(key))
            .header(HttpHeaders.CONTENT_LENGTH, length.toString())
            .header(HttpHeaders.CONTENT_RANGE, "bytes $start-$end/$totalLength")
            .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
            .header(HttpHeaders.PRAGMA, "no-cache")
            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
            .body(bytes)
    }
}
