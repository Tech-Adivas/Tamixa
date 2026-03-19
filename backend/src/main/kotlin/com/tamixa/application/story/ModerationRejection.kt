package com.tamixa.application.story

/**
 * Moderation layer identifier. Used to log which layer triggered rejection
 * for safety auditing and incident response.
 */
enum class ModerationLayer {
    /** Layer 1: OpenAI Moderation API */
    OPENAI_API,

    /** Layer 2: Custom configurable keyword blocklist */
    CUSTOM_KEYWORD_BLOCKLIST,

    /** Layer 3: Regex-based red-flag pattern detection */
    PATTERN_DETECTION,

    /** Layer 4: Age-based vocabulary restriction */
    AGE_VOCABULARY
}
