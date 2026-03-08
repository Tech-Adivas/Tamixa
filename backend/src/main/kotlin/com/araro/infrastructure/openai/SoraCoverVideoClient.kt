package com.araro.infrastructure.openai

import com.araro.application.port.CoverVideoGenerationPort
import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import java.awt.Graphics2D
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

/**
 * OpenAI Sora image-to-video adapter for story cover videos.
 * Resizes image to 1280x720, submits to POST /v1/videos, polls until completed, returns MP4 bytes.
 * Enable with app.sora.enabled=true and same OPENAI_API_KEY (Sora access required).
 */
@Component
@ConditionalOnProperty(name = ["app.sora.enabled"], havingValue = "true")
class SoraCoverVideoClient(
    private val restTemplate: RestTemplate,
    @Value("\${app.openai.api-key:}") private val apiKey: String,
    @Value("\${app.openai.base-url:https://api.openai.com}") private val baseUrl: String,
    @Value("\${app.sora.model:sora-2}") private val model: String,
    @Value("\${app.sora.seconds:4}") private val seconds: String,
    @Value("\${app.sora.poll-interval-sec:20}") private val pollIntervalSec: Long,
    @Value("\${app.sora.max-wait-sec:300}") private val maxWaitSec: Long
) : CoverVideoGenerationPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun generateVideoFromImage(imageBytes: ByteArray, motionPrompt: String): ByteArray? {
        if (apiKey.isBlank()) {
            log.warn("Sora skipped: OPENAI_API_KEY is blank")
            return null
        }
        val resized = resizeTo1280x720(imageBytes) ?: run {
            log.warn("Sora skipped: failed to resize image to 1280x720")
            return null
        }
        val videoId = createVideoJob(resized, motionPrompt) ?: return null
        val status = pollUntilCompleted(videoId) ?: return null
        if (status != "completed") {
            log.warn("Sora video job {} ended with status={}", videoId, status)
            return null
        }
        return downloadVideoContent(videoId)
    }

    private fun resizeTo1280x720(imageBytes: ByteArray): ByteArray? {
        return try {
            val input = ImageIO.read(ByteArrayInputStream(imageBytes))
                ?: return null
            val scaled = input.getScaledInstance(1280, 720, Image.SCALE_SMOOTH)
            val out = BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB)
            val g: Graphics2D = out.createGraphics()
            g.drawImage(scaled, 0, 0, null)
            g.dispose()
            val baos = ByteArrayOutputStream()
            ImageIO.write(out, "png", baos)
            baos.toByteArray()
        } catch (e: Exception) {
            log.warn("Image resize failed: {}", e.message)
            null
        }
    }

    private fun createVideoJob(imageBytes: ByteArray, motionPrompt: String): String? {
        return try {
            val imageResource = object : ByteArrayResource(imageBytes) {
                override fun getFilename(): String = "cover.png"
            }
            val fileHeaders = HttpHeaders().apply {
                contentType = MediaType.IMAGE_PNG
            }
            val body = LinkedMultiValueMap<String, Any>().apply {
                add("prompt", motionPrompt.take(800))
                add("model", model)
                add("size", "1280x720")
                add("seconds", seconds)
                add("input_reference", HttpEntity(imageResource, fileHeaders))
            }
            val headers = HttpHeaders().apply {
                setBearerAuth(apiKey)
                contentType = MediaType.MULTIPART_FORM_DATA
            }
            val response = restTemplate.postForObject(
                "$baseUrl/v1/videos",
                HttpEntity(body, headers),
                JsonNode::class.java
            )
            val id = response?.get("id")?.asText()
            if (id.isNullOrBlank()) {
                val error = response?.get("error")?.let { e -> "code=${e.get("code")?.asText()} message=${e.get("message")?.asText()}" }
                log.warn("Sora create video returned no id. response={} error={}", response?.toString()?.take(500), error)
                return null
            }
            log.info("Sora video job created id={}", id)
            id
        } catch (e: Exception) {
            log.error("Sora create video failed: {} cause={}", e.message, e.cause?.message, e)
            null
        }
    }

    private fun pollUntilCompleted(videoId: String): String? {
        val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(maxWaitSec)
        while (System.currentTimeMillis() < deadline) {
            val status = getVideoStatus(videoId) ?: return null
            when (status) {
                "completed" -> return "completed"
                "failed" -> {
                    log.warn("Sora video job {} failed", videoId)
                    return "failed"
                }
                else -> {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(pollIntervalSec))
                }
            }
        }
        log.warn("Sora video job {} timed out after {}s", videoId, maxWaitSec)
        return null
    }

    private fun getVideoStatus(videoId: String): String? {
        return try {
            val headers = HttpHeaders().apply {
                setBearerAuth(apiKey)
                contentType = MediaType.APPLICATION_JSON
            }
            val response = restTemplate.exchange(
                "$baseUrl/v1/videos/$videoId",
                HttpMethod.GET,
                HttpEntity<Void>(headers),
                JsonNode::class.java
            ).body
            response?.get("status")?.asText()
        } catch (e: Exception) {
            log.warn("Sora get status failed for {}: {}", videoId, e.message)
            null
        }
    }

    private fun downloadVideoContent(videoId: String): ByteArray? {
        return try {
            val headers = HttpHeaders().apply {
                setBearerAuth(apiKey)
            }
            val response: ResponseEntity<ByteArray> = restTemplate.exchange(
                "$baseUrl/v1/videos/$videoId/content",
                HttpMethod.GET,
                HttpEntity<Void>(headers),
                ByteArray::class.java
            )
            val bytes = response.body ?: run {
                log.warn("Sora video content empty for {}", videoId)
                return null
            }
            if (bytes.isEmpty()) {
                log.warn("Sora video content empty for {}", videoId)
                return null
            }
            log.info("Sora video downloaded {} bytes for {}", bytes.size, videoId)
            bytes
        } catch (e: Exception) {
            log.warn("Sora download content failed for {}: {}", videoId, e.message)
            null
        }
    }
}
