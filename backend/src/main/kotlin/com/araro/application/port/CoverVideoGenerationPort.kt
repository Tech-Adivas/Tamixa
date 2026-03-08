package com.araro.application.port

/**
 * Generates a short video from an image (e.g. DALL-E cover) using an image-to-video API (e.g. OpenAI Sora).
 * Returns MP4 bytes or null if disabled/fails.
 */
interface CoverVideoGenerationPort {

    /**
     * Generate a 2–5 second video using the given image as first frame and prompt for motion.
     * Image should be 1280x720 (caller or implementation may resize). Returns MP4 bytes or null.
     */
    fun generateVideoFromImage(imageBytes: ByteArray, motionPrompt: String): ByteArray?
}
