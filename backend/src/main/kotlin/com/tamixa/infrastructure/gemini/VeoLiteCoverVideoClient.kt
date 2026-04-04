package com.tamixa.infrastructure.gemini

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.tamixa.application.port.CoverVideoGenerationPort
import com.tamixa.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.awt.Graphics2D
import java.awt.Image
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import java.util.Base64
import javax.imageio.ImageIO

/**
 * [CoverVideoGenerationPort] using **Veo 3.1 Lite** (Gemini API `predictLongRunning` + poll + download).
 * Animates the cover still image with a motion prompt; returns MP4 bytes for FFmpeg → GIF.
 *
 * @see [Google video generation docs](https://ai.google.dev/gemini-api/docs/video)
 */
@Component
@ConditionalOnProperty(name = ["app.cover-animation.veo.enabled"], havingValue = "true")
class VeoLiteCoverVideoClient(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    private val appProperties: AppProperties,
) : CoverVideoGenerationPort {

    private val log = LoggerFactory.getLogger(javaClass)

    private val cfg get() = appProperties.coverAnimation.veo

    override fun generateVideoFromImage(imageBytes: ByteArray, motionPrompt: String): ByteArray? {
        val apiKey = appProperties.llm.gemini.apiKey.trim()
        if (apiKey.isEmpty()) {
            log.warn("Veo cover video skipped: GEMINI_API_KEY is blank")
            return null
        }
        if (imageBytes.isEmpty()) {
            log.warn("Veo cover video skipped: empty image")
            return null
        }
        return try {
            runVeoJob(apiKey, imageBytes, motionPrompt)
        } catch (e: Exception) {
            log.warn("Veo cover video failed: {}", e.message)
            null
        }
    }

    private fun runVeoJob(apiKey: String, imageBytes: ByteArray, motionPrompt: String): ByteArray? {
        val (frameBytes, mimeType) = prepareCoverFrame(imageBytes)
        val base = cfg.baseUrl.trimEnd('/')
        val model = cfg.model.trim().removePrefix("models/")
        val duration = normalizeDuration(cfg.durationSeconds)
        val prompt = motionPrompt.trim().take(PROMPT_MAX_CHARS)

        val instance: Map<String, Any> = mapOf(
            "prompt" to prompt,
            "image" to mapOf(
                "inlineData" to mapOf(
                    "mimeType" to mimeType,
                    "data" to Base64.getEncoder().encodeToString(frameBytes),
                ),
            ),
        )
        val parameters: Map<String, String> = mapOf(
            "aspectRatio" to cfg.aspectRatio.trim().ifBlank { "16:9" },
            "durationSeconds" to duration,
            "resolution" to cfg.resolution.trim().ifBlank { "720p" },
            "personGeneration" to normalizePersonGeneration(cfg.personGeneration),
        )
        val requestBody = mapOf(
            "instances" to listOf(instance),
            "parameters" to parameters,
        )
        val json = objectMapper.writeValueAsString(requestBody)
        val headers = HttpHeaders().apply {
            set("x-goog-api-key", apiKey)
            contentType = MediaType.APPLICATION_JSON
        }
        val startUrl = "$base/v1beta/models/$model:predictLongRunning"
        val startEntity = HttpEntity(json, headers)
        val startResponse: ResponseEntity<String> = try {
            restTemplate.exchange(startUrl, HttpMethod.POST, startEntity, String::class.java)
        } catch (e: Exception) {
            log.error("Veo predictLongRunning HTTP error: {}", e.message, e)
            return null
        }
        val startBody = startResponse.body ?: run {
            log.warn("Veo predictLongRunning returned empty body")
            return null
        }
        val startTree = objectMapper.readTree(startBody)
        if (startTree.has("error")) {
            log.warn("Veo predictLongRunning API error: {}", startTree.path("error").toString().take(500))
            return null
        }
        val operationName = startTree.path("name").textValue()?.trim()
        if (operationName.isNullOrEmpty()) {
            log.warn("Veo predictLongRunning missing operation name: {}", startBody.take(400))
            return null
        }
        val pollUrl = if (operationName.startsWith("http")) {
            operationName
        } else {
            "$base/$operationName"
        }
        val deadline = System.currentTimeMillis() + cfg.maxWaitSec.coerceIn(30, 900) * 1000L
        val intervalMs = cfg.pollIntervalSec.coerceIn(3, 60) * 1000L
        var lastBody: String? = null
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(intervalMs)
            val statusResponse = try {
                restTemplate.exchange(
                    URI.create(pollUrl),
                    HttpMethod.GET,
                    HttpEntity<Any>(null, headers),
                    String::class.java,
                ).body
            } catch (e: Exception) {
                log.warn("Veo operation poll failed: {}", e.message)
                continue
            }
            lastBody = statusResponse
            if (statusResponse.isNullOrEmpty()) continue
            val node = objectMapper.readTree(statusResponse)
            if (node.path("error").isObject) {
                log.warn("Veo operation error: {}", node.path("error").toString().take(500))
                return null
            }
            if (!node.path("done").asBoolean(false)) continue
            if (node.path("error").isObject) {
                log.warn("Veo operation completed with error: {}", node.path("error").toString().take(500))
                return null
            }
            val videoUri = extractVideoDownloadUri(node)
            if (videoUri.isNullOrBlank()) {
                log.warn("Veo done but no video URI in response: {}", statusResponse.take(600))
                return null
            }
            return downloadVideo(apiKey, videoUri)
        }
        log.warn("Veo operation timed out after {}s; last={}", cfg.maxWaitSec, lastBody?.take(300))
        return null
    }

    private fun downloadVideo(apiKey: String, videoUri: String): ByteArray? {
        val headers = HttpHeaders().apply { set("x-goog-api-key", apiKey) }
        return try {
            val entity = HttpEntity<Any>(null, headers)
            val uri = URI.create(videoUri)
            val bytes = restTemplate.exchange(uri, HttpMethod.GET, entity, ByteArray::class.java).body
            if (bytes == null || bytes.isEmpty()) {
                log.warn("Veo video download empty")
                null
            } else {
                log.info("Veo video downloaded {} bytes", bytes.size)
                bytes
            }
        } catch (e: Exception) {
            log.error("Veo video download failed: {}", e.message, e)
            null
        }
    }

    private fun extractVideoDownloadUri(doneNode: JsonNode): String? {
        val response = doneNode.path("response")
        val samples = response.path("generateVideoResponse").path("generatedSamples")
        if (samples.isArray && samples.size() > 0) {
            val uri = samples[0].path("video").path("uri").textValue()?.trim()
            if (!uri.isNullOrEmpty()) return uri
        }
        // Alternate shapes (defensive)
        val alt = response.path("generatedVideos")
        if (alt.isArray && alt.size() > 0) {
            val uri = alt[0].path("video").path("uri").textValue()?.trim()
            if (!uri.isNullOrEmpty()) return uri
        }
        return null
    }

    private fun normalizeDuration(raw: String): String {
        val s = raw.trim()
        return when (s) {
            "4", "6", "8" -> s
            else -> {
                log.debug("Veo durationSeconds {} invalid; using 4", raw)
                "4"
            }
        }
    }

    private fun normalizePersonGeneration(raw: String): String {
        val s = raw.trim().lowercase()
        // Image-to-video: Google documents allow_adult for this path
        return when (s) {
            "allow_adult", "allow_all" -> "allow_adult"
            else -> "allow_adult"
        }
    }

    private fun prepareCoverFrame(imageBytes: ByteArray): Pair<ByteArray, String> {
        val mime = detectMimeType(imageBytes)
        return try {
            val input = ByteArrayInputStream(imageBytes)
            val original = ImageIO.read(input)
            if (original == null) {
                log.debug("ImageIO could not read cover frame; sending original bytes as {}", mime)
                return imageBytes to mime
            }
            val scaled = scaleAndCropTo16x9(original, TARGET_W, TARGET_H)
            val out = ByteArrayOutputStream()
            ImageIO.write(scaled, "png", out)
            out.toByteArray() to "image/png"
        } catch (e: Exception) {
            log.warn("Cover frame resize failed ({}); using original bytes", e.message)
            imageBytes to mime
        }
    }

    private fun detectMimeType(bytes: ByteArray): String {
        if (bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) {
            return "image/jpeg"
        }
        if (bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
        ) {
            return "image/png"
        }
        return "image/png"
    }

    private fun scaleAndCropTo16x9(src: BufferedImage, targetW: Int, targetH: Int): BufferedImage {
        val sw = src.width.coerceAtLeast(1)
        val sh = src.height.coerceAtLeast(1)
        val scale = maxOf(targetW.toDouble() / sw, targetH.toDouble() / sh)
        val nw = (sw * scale).toInt().coerceAtLeast(1)
        val nh = (sh * scale).toInt().coerceAtLeast(1)
        val scaled = BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB)
        val g1: Graphics2D = scaled.createGraphics()
        try {
            g1.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g1.drawImage(src.getScaledInstance(nw, nh, Image.SCALE_SMOOTH), 0, 0, null)
        } finally {
            g1.dispose()
        }
        val x = (nw - targetW) / 2
        val y = (nh - targetH) / 2
        val out = BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB)
        val g2: Graphics2D = out.createGraphics()
        try {
            g2.drawImage(scaled, 0, 0, targetW, targetH, x, y, x + targetW, y + targetH, null)
        } finally {
            g2.dispose()
        }
        return out
    }

    private companion object {
        const val TARGET_W = 1280
        const val TARGET_H = 720
        const val PROMPT_MAX_CHARS = 1500
    }
}
