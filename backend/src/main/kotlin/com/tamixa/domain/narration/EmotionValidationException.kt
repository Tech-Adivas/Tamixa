package com.tamixa.domain.narration

/**
 * Thrown when emotion-tagged script fails validation.
 * Prevents prosody from being applied to scripts where word count deviates
 * too much—indicating potential content alteration.
 */
class EmotionValidationException(message: String) : RuntimeException(message)
