package com.tamixa.infrastructure.narration

import com.tamixa.application.narration.AudioStorageService
import com.tamixa.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.File
import java.nio.file.Files

/**
 * Stores narration audio in S3.
 * Path: stories/{storyId}/{language}/{voiceProfileSlug}.mp3
 * Enable with app.storage.type=s3.
 *
 * Self-hosted XTTS returns WAV (RIFF); clients expect MP3. If input is WAV, transcode with FFmpeg
 * before upload so ExoPlayer/AVPlayer decode real MP3 (WAV-as-MP3 sounds like noise/hiss only).
 */
@Component
@ConditionalOnProperty(name = ["app.storage.type"], havingValue = "s3")
class S3NarrationAudioStorageAdapter(
    private val s3Client: S3Client,
    private val appProperties: AppProperties
) : AudioStorageService {

    private val log = LoggerFactory.getLogger(javaClass)
    private val bucket get() = appProperties.storage.effectiveS3Bucket

    override fun uploadNarrationAudio(
        storyId: Long,
        language: String,
        voiceProfile: String,
        mp3Bytes: ByteArray
    ): String {
        val slug = if (voiceProfile == "default") "v1" else voiceProfileSlug(voiceProfile)
        val key = "stories/$storyId/${sanitizeLanguage(language)}/$slug.mp3"
        val payload = ensureMp3Bytes(mp3Bytes, storyId, key)
        log.debug("S3 upload start storyId={} key={} bytes={}", storyId, key, payload.size)
        return try {
            val request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("audio/mpeg")
                .build()
            s3Client.putObject(request, RequestBody.fromBytes(payload))
            log.info("S3 upload success storyId={} key={} bytes={}", storyId, key, payload.size)
            key
        } catch (e: Exception) {
            val msg = e.message ?: e.cause?.message ?: "Unknown error"
            log.error("S3 upload failed storyId={} key={} error={}", storyId, key, msg, e)
            throw IllegalStateException("Audio storage failed: ${msg.take(250)}", e)
        }
    }

    private fun voiceProfileSlug(profile: String): String =
        profile.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(50).ifBlank { "v1" }

    private fun sanitizeLanguage(lang: String): String =
        lang.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(16).ifBlank { "default" }

    /**
     * XTTS/ElevenLabs mismatch: XTTS returns WAV. Uploading WAV bytes with .mp3 key + audio/mpeg
     * makes players decode garbage. Convert RIFF/WAVE to MP3 when needed.
     */
    private fun ensureMp3Bytes(bytes: ByteArray, storyId: Long, key: String): ByteArray {
        if (bytes.size < 12) return bytes
        val isWav = bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'A'.code.toByte() &&
            bytes[10] == 'V'.code.toByte() && bytes[11] == 'E'.code.toByte()
        if (!isWav) return bytes
        var wavFile: File? = null
        var mp3File: File? = null
        try {
            wavFile = Files.createTempFile("narration-", ".wav").toFile()
            mp3File = Files.createTempFile("narration-", ".mp3").toFile()
            wavFile.writeBytes(bytes)
            val process = ProcessBuilder(
                "ffmpeg", "-y", "-i", wavFile.absolutePath,
                "-codec:a", "libmp3lame", "-q:a", "4",
                "-ac", "1", "-ar", "44100",
                mp3File.absolutePath
            ).redirectErrorStream(true).start()
            val exit = process.waitFor()
            if (exit != 0) {
                val err = process.inputStream.bufferedReader().readText()
                log.error("WAV→MP3 ffmpeg exit={} storyId={} key={} err={}", exit, storyId, key, err.take(400))
                throw IllegalStateException("Narration audio is WAV but FFmpeg WAV→MP3 failed. Install ffmpeg (brew install ffmpeg).")
            }
            if (!mp3File.exists() || mp3File.length() == 0L) {
                throw IllegalStateException("FFmpeg did not produce MP3 output")
            }
            val mp3 = mp3File.readBytes()
            log.info("Transcoded WAV ({} bytes) to MP3 ({} bytes) for key={}", bytes.size, mp3.size, key)
            return mp3
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            log.error("WAV→MP3 failed storyId={} key={}: {}", storyId, key, e.message, e)
            throw IllegalStateException("Narration storage requires FFmpeg to transcode XTTS WAV to MP3: ${e.message}", e)
        } finally {
            wavFile?.delete()
            mp3File?.delete()
        }
    }
}
