package com.tamixa.infrastructure.narration

import com.mpatric.mp3agic.Mp3File
import org.slf4j.LoggerFactory
import java.nio.file.Files

/** Shared threshold: measured audio shorter than this vs script-based expected speech (~150 wpm) → truncation_warning. */
object NarrationTruncationPolicy {
    const val MIN_AUDIO_VS_EXPECTED_SPEECH_FRACTION = 0.48
}

/**
 * Reads playback length from MP3 bytes (curated pipeline TTS output).
 * Used for accurate [com.tamixa.domain.narration.StoryNarrationAudio.durationSeconds] and truncation detection.
 */
object Mp3DurationReader {
    private val log = LoggerFactory.getLogger(javaClass)

    fun durationSecondsOrNull(mp3Bytes: ByteArray): Int? {
        if (mp3Bytes.isEmpty()) return null
        val tmp = try {
            Files.createTempFile("tamixa-mp3-", ".mp3")
        } catch (e: Exception) {
            log.warn("MP3 duration: could not create temp file: {}", e.message)
            return null
        }
        return try {
            Files.write(tmp, mp3Bytes)
            val mp3 = Mp3File(tmp.toFile())
            mp3.lengthInSeconds.toInt().coerceAtLeast(1)
        } catch (e: Exception) {
            log.debug("MP3 duration parse failed ({} bytes): {}", mp3Bytes.size, e.message)
            null
        } finally {
            try {
                Files.deleteIfExists(tmp)
            } catch (_: Exception) {
                // best-effort cleanup
            }
        }
    }
}
