package com.araro.application.story

import org.springframework.stereotype.Component

/**
 * Custom safety validation rules for generated stories: harmful language,
 * adult themes, complex psychological themes, and age-appropriate fear control.
 * If validation fails, caller should retry with fallback prompt; if still fails, mark story FAILED.
 */
@Component
class StorySafetyValidationRules {

    private val harmfulAndAdult = setOf(
        "violence", "weapon", "kill", "murder", "blood", "gore",
        "political", "election", "party", "vote", "propaganda",
        "drug", "alcohol", "suicide", "self-harm", "sexual", "nude"
    ).map { it.lowercase() }

    private val complexPsychological = setOf(
        "depression", "anxiety", "trauma", "ptsd", "panic attack",
        "breakdown", "insanity", "hallucination"
    ).map { it.lowercase() }

    /** Fear-inducing words not allowed for age < 6. */
    private val fearWordsYoung = setOf(
        "ghost", "monster", "scary", "terrifying", "scream", "screaming",
        "darkness", "witch", "haunted", "nightmare", "horror", "afraid", "frightened"
    ).map { it.lowercase() }

    /**
     * Runs all safety rules. Throws [ContentModerationException] or [InvalidStoryRequestException] if invalid.
     * Pass [age] for age-appropriate fear-word check (under 6: no fear words).
     */
    fun validate(content: String, age: Int) {
        val lower = content.lowercase()
        harmfulAndAdult.find { lower.contains(it) }?.let {
            throw ContentModerationException("Generated story contains harmful or adult content")
        }
        complexPsychological.find { lower.contains(it) }?.let {
            throw ContentModerationException("Generated story contains complex psychological themes")
        }
        if (age < 6) {
            fearWordsYoung.find { lower.contains(it) }?.let {
                throw ContentModerationException("Story contains fear-inducing words not suitable for age under 6")
            }
        }
    }
}
