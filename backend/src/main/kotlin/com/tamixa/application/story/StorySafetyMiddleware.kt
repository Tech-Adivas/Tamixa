package com.tamixa.application.story

import com.tamixa.infrastructure.config.AppProperties
import org.springframework.stereotype.Component

/**
 * Safety middleware for story generation: prompt injection prevention,
 * input sanitization, theme allowlist, control token blocking, and child-safe vocabulary enforcement.
 *
 * Safety logic:
 * - System prompt and user inputs are strictly separated (enforced by StoryPromptBuilder).
 * - All user inputs (theme, childName) are sanitized before use.
 * - Theme allowlist: if configured, theme must match; else any theme passes.
 * - Control tokens (null bytes, instruction-override patterns) are blocked.
 * - User-controlled instruction override attempts are rejected.
 */
@Component
class StorySafetyMiddleware(
    private val appProperties: AppProperties
) {

    /** Max length for theme and childName to limit prompt injection surface. */
    private val maxThemeLength = 100
    private val maxChildNameLength = 50
    private val maxParentCustomPromptLength = 200

    /**
     * Patterns that may indicate prompt injection or user-controlled instruction override.
     * Blocks attempts to override system instructions.
     */
    private val injectionPatterns = listOf(
        Regex("(?i)ignore\\s+(previous|above|all|all\\s+instructions)"),
        Regex("(?i)disregard\\s+(previous|instructions)"),
        Regex("(?i)you\\s+are\\s+now"),
        Regex("(?i)new\\s+instructions?\\s*:"),
        Regex("(?i)system\\s*:\\s*"),
        Regex("(?i)user\\s*:\\s*.*(assistant|instruction)"),
        Regex("(?i)<\\s*script"),
        Regex("(?i)\\{\\s*\\{.*\\}\\s*\\}"),
        Regex("""(?i)\b(jailbreak|override|bypass|prompt\s+leak)\b"""),
        Regex("(?i)forget\\s+(everything|your|the)\\s+(instructions|rules)"),
        Regex("(?i)act\\s+as\\s+(a\\s+)?(different|new)\\s+")
    )

    /**
     * Control characters that could confuse the model or enable injection.
     * Allow \n, \t, \r; block null, other C0 controls, and Unicode format chars.
     */
    private val controlCharPattern = Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\u200B-\\u200D\\u2060\\uFEFF]")

    /** Child-unsafe terms that must not appear in user input or generated content. */
    private val childUnsafeBlocklist = setOf(
        "violence", "weapon", "kill", "murder", "blood", "gore",
        "political", "election", "party", "vote", "propaganda",
        "drug", "alcohol", "suicide", "self-harm", "religious conflict"
    ).map { it.lowercase() }

    /** Default allowed themes when allowlist is empty (backward compat: allow any). */
    private fun allowedThemes(): Set<String> {
        val configured = appProperties.story.themeAllowlist
        if (configured.isBlank()) return emptySet()  // empty = no restriction
        return configured.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
    }

    /**
     * Sanitizes and validates theme and child name.
     * - Strips control tokens.
     * - Enforces theme allowlist when configured.
     * - Rejects prompt injection patterns.
     * - Rejects blocklisted vocabulary.
     */
    fun sanitizeAndValidateInput(theme: String, childName: String, trustedCatalogTheme: Boolean = false): SanitizedInput {
        // 1. Strip control characters (prevents control-token injection)
        val sanitizedTheme = stripControlTokens(theme.trim().take(maxThemeLength))
        val sanitizedChildName = stripControlTokens(childName.trim().take(maxChildNameLength))

        if (sanitizedTheme.isBlank()) throw InvalidStoryRequestException("Theme is required")
        if (sanitizedChildName.isBlank()) throw InvalidStoryRequestException("Child name is required")

        // 2. Theme allowlist: if configured, theme must match an allowed value (skip for server catalog themes)
        if (!trustedCatalogTheme) {
            val allowlist = allowedThemes()
            if (allowlist.isNotEmpty()) {
                val themeLower = sanitizedTheme.lowercase()
                val matched = allowlist.any { allowed ->
                    themeLower == allowed || themeLower.contains(allowed) || allowed.contains(themeLower)
                }
                if (!matched) {
                    throw InvalidStoryRequestException("Theme not allowed. Please choose a child-friendly theme.")
                }
            }
        }

        // 3. Block prompt injection / instruction override
        val combined = "$sanitizedTheme $sanitizedChildName"
        injectionPatterns.forEach { pattern ->
            if (pattern.containsMatchIn(combined)) {
                throw ContentModerationException("Request rejected: invalid or unsafe input")
            }
        }

        // 4. Block child-unsafe vocabulary in user input (theme exempt when from approved catalog)
        if (!trustedCatalogTheme) {
            val lower = combined.lowercase()
            childUnsafeBlocklist.find { lower.contains(it) }?.let {
                throw ContentModerationException("Request rejected: content not allowed for children")
            }
        } else {
            val lowerName = sanitizedChildName.lowercase()
            childUnsafeBlocklist.find { lowerName.contains(it) }?.let {
                throw ContentModerationException("Request rejected: content not allowed for children")
            }
        }

        return SanitizedInput(sanitizedTheme, sanitizedChildName)
    }

    /** Max length per conversation message. */
    private val maxConversationMessageLength = 300
    private val maxConversationMessages = 10

    /**
     * Sanitizes conversation messages for summarization.
     * Returns empty list if null; filters and truncates each message.
     */
    fun sanitizeConversationMessages(messages: List<String>?): List<String> {
        if (messages.isNullOrEmpty()) return emptyList()
        return messages
            .take(maxConversationMessages)
            .mapNotNull { msg ->
                val sanitized = stripControlTokens(msg.trim().take(maxConversationMessageLength))
                if (sanitized.isBlank()) null
                else {
                    injectionPatterns.forEach { if (it.containsMatchIn(sanitized)) return@mapNotNull null }
                    if (childUnsafeBlocklist.any { sanitized.lowercase().contains(it) }) return@mapNotNull null
                    sanitized
                }
            }
    }

    /**
     * Phase 2: Sanitizes parent custom prompt for inclusion in story generation.
     * Returns null if input is blank or fails validation.
     */
    fun sanitizeParentCustomPrompt(input: String?): String? {
        if (input.isNullOrBlank()) return null
        val sanitized = stripControlTokens(input.trim().take(maxParentCustomPromptLength))
        if (sanitized.isBlank()) return null
        injectionPatterns.forEach { pattern ->
            if (pattern.containsMatchIn(sanitized)) return null
        }
        val lower = sanitized.lowercase()
        if (childUnsafeBlocklist.any { lower.contains(it) }) return null
        return sanitized
    }

    /** Removes control tokens and unexpected unicode that could affect model behavior. */
    private fun stripControlTokens(input: String): String = controlCharPattern.replace(input, "")

    /**
     * Checks if generated story text contains blocklisted vocabulary.
     * Returns true if safe, false if blocklisted term found.
     */
    fun isGeneratedContentChildSafe(text: String): Boolean {
        val lower = text.lowercase()
        return !childUnsafeBlocklist.any { lower.contains(it) }
    }
}

data class SanitizedInput(val theme: String, val childName: String)
