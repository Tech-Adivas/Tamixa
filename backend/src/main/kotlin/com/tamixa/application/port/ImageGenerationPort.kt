package com.tamixa.application.port

/**
 * Generates images from text prompts (e.g. DALL-E). Returns raw bytes for storage.
 */
interface ImageGenerationPort {

    /**
     * Generate a child-safe illustration from a prompt. Returns PNG bytes or null if disabled/fails.
     */
    fun generateImage(prompt: String): ByteArray?
}
