package com.tamixa.application.story

/**
 * Shared text-to-image prompt wrapper for story covers (DALL·E, Gemini image, etc.):
 * child-safe, story-faithful, no on-image text.
 */
object CoverIllustrationPrompts {

    /**
     * Safety/style wrapper that keeps visuals child-safe while preserving story intent.
     * Scene text is capped so total prompt stays within typical model limits (e.g. DALL·E ~4k chars).
     */
    fun buildChildSafeCoverPrompt(theme: String): String {
        val safe = theme.take(3200).replace(Regex("[^\\p{L}\\p{N}\\s.,'-]"), " ")
        return (
            "Create a premium storybook cover illustration for Tamixa app. " +
                "Follow the story scene details exactly; do not default to generic temple, child portrait, or gold-tinted composition unless explicitly described. " +
                "Keep it child-safe, calm, and emotionally warm (no violence, gore, fear, weapons, or distress). " +
                "Use a refined, modern storybook style (not cartoonish, not photoreal). " +
                "Color direction: derive colors from the story mood and setting while staying compatible with dark app UI (balanced contrast, no harsh neon, no blown-out whites). " +
                "No text, words, logos, or letters in the image. High-definition square cover. " +
                "Story scene input: $safe."
            )
    }
}
