package com.tamixa.application.port

/**
 * Converts MP4 video bytes to animated GIF (e.g. after an image-to-video cover step).
 * Used so the app can show GIF-formatted animation instead of video.
 */
interface VideoToGifConverterPort {

    /**
     * Convert MP4 bytes to GIF. Returns GIF bytes or null if conversion fails / not available.
     */
    fun convertMp4ToGif(mp4Bytes: ByteArray): ByteArray?
}
