package com.tamixa.infrastructure.shareclip

import com.tamixa.application.port.ShareClipRenderPort
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

/**
 * Renders share clip using FFmpeg: extract audio segment, composite with cover image, add watermark.
 * Requires ffmpeg in PATH. Set SHARE_CLIP_FFMPEG_ENABLED=true to use.
 */
@Component
@ConditionalOnProperty(name = ["app.share-clip.ffmpeg-enabled"], havingValue = "true")
class FFmpegShareClipRenderAdapter : ShareClipRenderPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun renderClip(
        audioUrl: String,
        coverImageUrl: String,
        startSeconds: Int,
        durationSeconds: Int,
        format: String
    ): ByteArray? {
        var audioFile: Path? = null
        var coverFile: Path? = null
        var outputFile: Path? = null
        return try {
            audioFile = downloadToTemp(audioUrl, "audio", ".mp3")
            coverFile = downloadToTemp(coverImageUrl, "cover", ".png")
            if (audioFile == null || coverFile == null) {
                log.warn("Failed to download audio or cover for clip render")
                return null
            }
            outputFile = Files.createTempFile("share_clip", ".mp4")
            val (width, height) = when (format) {
                "1:1" -> 1080 to 1080
                else -> 1080 to 1920  // 9:16
            }
            val watermarkText = "Made with Tamixa"
            val cmd = listOf(
                "ffmpeg", "-y",
                "-ss", startSeconds.toString(),
                "-t", durationSeconds.toString(),
                "-i", audioFile!!.toString(),
                "-loop", "1", "-i", coverFile!!.toString(),
                "-vf", "scale=$width:$height:force_original_aspect_ratio=decrease,pad=$width:$height:(ow-iw)/2:(oh-ih)/2,drawtext=text='$watermarkText':fontsize=24:fontcolor=white:x=(w-text_w)/2:y=h-50",
                "-c:v", "libx264", "-tune", "stillimage", "-c:a", "aac", "-shortest",
                outputFile!!.toString()
            )
            val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                log.warn("FFmpeg failed exitCode={} output={}", exitCode, output.take(500))
                return null
            }
            Files.readAllBytes(outputFile)
        } catch (e: Exception) {
            log.error("Share clip FFmpeg render failed: {}", e.message, e)
            null
        } finally {
            audioFile?.let { Files.deleteIfExists(it) }
            coverFile?.let { Files.deleteIfExists(it) }
            outputFile?.let { Files.deleteIfExists(it) }
        }
    }

    private fun downloadToTemp(url: String, prefix: String, suffix: String): Path? {
        return try {
            val bytes = URL(url).openStream().readAllBytes()
            val file = Files.createTempFile(prefix, suffix)
            Files.write(file, bytes)
            file
        } catch (e: Exception) {
            log.warn("Download failed url={}: {}", url.take(80), e.message)
            null
        }
    }
}
