package com.araro.application.port

/**
 * Converts MP4 video bytes to animated GIF (e.g. for Sora cover output).
 * Used so the app can show GIF-formatted animation instead of video.
 */
interface VideoToGifConverterPort {

    /**
     * Convert MP4 bytes to GIF. Returns GIF bytes or null if conversion fails / not available.
     */
    fun convertMp4ToGif(mp4Bytes: ByteArray): ByteArray?
}
