package com.tamixa.application.port

/**
 * Generates images from text prompts (e.g. OpenAI DALL·E 3 or Gemini native image). Returns raw bytes for storage.
 */
interface ImageGenerationPort {

    /**
     * Generate a child-safe illustration from a prompt. Returns PNG bytes or null if disabled/fails.
     */
    fun generateImage(prompt: String): ByteArray?

    /**
     * Story slideshow picture (portrait), drawn to match [references] (e.g. the character sheet and the cover) so
     * characters look the same in every picture. Returns image bytes, or null when the provider can't do this.
     */
    fun generateStoryScene(prompt: String, references: List<ByteArray> = emptyList()): ByteArray? = null
}
