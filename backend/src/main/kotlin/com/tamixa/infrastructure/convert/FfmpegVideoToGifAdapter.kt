package com.tamixa.infrastructure.convert

import com.tamixa.application.port.VideoToGifConverterPort
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Files

/**
 * Converts MP4 to animated GIF using FFmpeg with high-quality settings.
 * Uses two-pass palette (palettegen + paletteuse) for accurate colors and brightness,
 * higher resolution (960px), and a slight contrast/brightness nudge so the GIF is not dim.
 * Requires FFmpeg on PATH (e.g. apt-get install ffmpeg / brew install ffmpeg).
 * Enable with app.sora.convert-to-gif=true (default true when Sora is used).
 */
@Component
@ConditionalOnProperty(name = ["app.sora.convert-to-gif"], havingValue = "true", matchIfMissing = true)
class FfmpegVideoToGifAdapter : VideoToGifConverterPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun convertMp4ToGif(mp4Bytes: ByteArray): ByteArray? {
        if (mp4Bytes.isEmpty()) return null
        var mp4File: File? = null
        var gifFile: File? = null
        try {
            mp4File = Files.createTempFile("tamixa-cover-", ".mp4").toFile()
            gifFile = Files.createTempFile("tamixa-cover-", ".gif").toFile()
            mp4File.writeBytes(mp4Bytes)
            // HD-like output: 960px width (source is 1280x720). 10 fps for smooth loop.
            // eq=contrast=1.06:brightness=0.04 keeps GIF bright and punchy (avoids dim look).
            // palettegen stats_mode=full + paletteuse with bayer dither for best quality vs size.
            val filter = (
                "fps=10,scale=960:-1:flags=lanczos," +
                "eq=contrast=1.06:brightness=0.04," +
                "split[s0][s1];[s0]palettegen=stats_mode=full[p];[s1][p]paletteuse=dither=bayer:bayer_scale=5"
            )
            val process = ProcessBuilder(
                "ffmpeg", "-y", "-i", mp4File.absolutePath,
                "-vf", filter,
                "-loop", "0",
                gifFile.absolutePath
            ).redirectErrorStream(true).start()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val err = process.inputStream.bufferedReader().readText()
                log.warn("FFmpeg exit {}: {}", exitCode, err.take(500))
                return null
            }
            if (!gifFile.exists() || gifFile.length() == 0L) {
                log.warn("FFmpeg did not produce GIF output")
                return null
            }
            val gifBytes = gifFile.readBytes()
            log.info("Converted MP4 ({} bytes) to GIF ({} bytes)", mp4Bytes.size, gifBytes.size)
            return gifBytes
        } catch (e: Exception) {
            log.warn("MP4 to GIF conversion failed: {} (is FFmpeg installed?)", e.message)
            return null
        } finally {
            mp4File?.delete()
            gifFile?.delete()
        }
    }
}
