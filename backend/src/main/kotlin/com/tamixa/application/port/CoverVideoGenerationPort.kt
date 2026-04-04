package com.tamixa.application.port

/**
 * Generates a short video from a cover still image for animated GIF covers.
 * Default implementation when enabled: [com.tamixa.infrastructure.gemini.VeoLiteCoverVideoClient] (Veo 3.1 Lite via Gemini API).
 * Returns MP4 bytes or null if disabled/fails.
 */
interface CoverVideoGenerationPort {

    /**
     * Generate a 2–5 second video using the given image as first frame and prompt for motion.
     * Image should be 1280x720 (caller or implementation may resize). Returns MP4 bytes or null.
     */
    fun generateVideoFromImage(imageBytes: ByteArray, motionPrompt: String): ByteArray?
}
