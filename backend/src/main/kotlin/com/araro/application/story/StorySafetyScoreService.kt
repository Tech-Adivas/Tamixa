package com.araro.application.story

import com.araro.infrastructure.config.AppProperties
import com.araro.infrastructure.observability.ApplicationMetrics
import org.springframework.stereotype.Service

/**
 * Computes StorySafetyScore (0-100) for generated stories.
 * - Deducts for risky patterns (violence hints, negative tone, complex psychology)
 * - Rewards for moral clarity (explicit moral, positive lesson)
 * - Rewards for positive tone (kindness, sharing, friendship keywords)
 *
 * Rejects story if score < threshold. Score stored in DB for auditing.
 */
@Service
class StorySafetyScoreService(
    private val appProperties: AppProperties,
    private val metrics: ApplicationMetrics
) {

    private val riskyPatterns = listOf(
        Regex("""(?i)\b(fight|angry|hate|scared|afraid|worried)\b""") to -5,
        Regex("""(?i)\b(hurt|pain|cry|crying|sad)\b""") to -3,
        Regex("""(?i)\b(weapon|violence|kill|blood)\b""") to -20,
        Regex("""(?i)\b(political|election|war)\b""") to -25
    )

    private val moralClarityPatterns = listOf(
        Regex("""(?i)\b(share|sharing|kindness|honest|honesty|helping|learned)\b""") to 5,
        Regex("""(?i)\b(friend|friendship|love|care|caring)\b""") to 3,
        Regex("""(?i)\b(moral|lesson|teach|teaches)\b""") to 5
    )

    private val positiveTonePatterns = listOf(
        Regex("""(?i)\b(happy|joy|smile|laughed|together)\b""") to 3,
        Regex("""(?i)\b(thank|thanks|grateful|wonderful)\b""") to 2
    )

    /**
     * Computes safety score for [payload]. Returns score in 0-100.
     * Throws if score < configured threshold.
     */
    fun computeAndValidate(payload: StructuredStoryPayload): Int {
        val fullText = "${payload.title} ${payload.moral} ${payload.storyText}"
        var score = 80  // Start at 80; deduct/reward from there

        // Deduct for risky patterns
        riskyPatterns.forEach { (pattern, penalty) ->
            val matches = pattern.findAll(fullText).count()
            if (matches > 0) score += penalty * matches
        }

        // Reward for moral clarity
        if (payload.moral.isNotBlank()) {
            score += 5  // Base reward for having a moral
            moralClarityPatterns.forEach { (pattern, bonus) ->
                if (pattern.containsMatchIn(payload.moral)) score += bonus
            }
        }

        // Reward for positive tone in story
        positiveTonePatterns.forEach { (pattern, bonus) ->
            val matches = pattern.findAll(payload.storyText).count()
            if (matches > 0) score += bonus * matches.coerceAtMost(3)  // Cap bonus
        }

        val finalScore = score.coerceIn(0, 100)

        // Record for observability
        metrics.recordSafetyScore(finalScore)

        val threshold = appProperties.story.safetyScoreThreshold
        if (finalScore < threshold) {
            throw ContentModerationException(
                "Story safety score ($finalScore) below threshold ($threshold)"
            )
        }

        return finalScore
    }
}
