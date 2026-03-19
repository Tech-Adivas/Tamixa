package com.tamixa.domain

/**
 * Types of short-form content for kids and families.
 * Stored as string in DB; use [value] for persistence and API.
 */
enum class ShortContentType(val value: String) {
    RIDDLE("RIDDLE"),
    THOUGHT_FOR_THE_DAY("THOUGHT_FOR_THE_DAY"),
    PROVERB("PROVERB"),
    TONGUE_TWISTER("TONGUE_TWISTER"),
    JOKE("JOKE"),
    FUN_FACT("FUN_FACT"),
    WORD_OF_THE_DAY("WORD_OF_THE_DAY"),
    BRAIN_TEASER("BRAIN_TEASER"),
    AFFIRMATION("AFFIRMATION"),
    QUOTE("QUOTE"),
    DID_YOU_KNOW("DID_YOU_KNOW"),
    RHYME("RHYME"),
    TRIVIA("TRIVIA");

    companion object {
        private val byValue = entries.associateBy { it.value }

        fun fromString(s: String?): ShortContentType? = s?.let { byValue[it.trim().uppercase()] }

        fun allValues(): List<String> = entries.map { it.value }
    }
}
