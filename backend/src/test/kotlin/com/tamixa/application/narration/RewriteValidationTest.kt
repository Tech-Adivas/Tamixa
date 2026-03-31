package com.tamixa.application.narration

import com.tamixa.application.narration.impl.SafetyValidatorServiceImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for rewrite (conversational transformation) validation.
 * Ensures SafetyValidatorService rejects invalid LLM outputs.
 */
class RewriteValidationTest {

    /** maxWords=0 disables hard cap so tests exercise ratio-only behaviour. */
    private val validator = SafetyValidatorServiceImpl(20, "badword,violence", 0)

    @Test
    fun `validates rewrite when word count within 20 percent of original`() {
        val original = "Once upon a time there was a little rabbit. It loved carrots."
        val rewrite = "Once upon a time there was a little rabbit who loved carrots very much."
        val result = validator.validate(
            SafetyValidationRequest(
                originalContent = original,
                originalMoral = null,
                formattedScript = rewrite,
                age = 5,
                language = "en"
            )
        )
        assertTrue(result.valid)
        assertEquals(100, result.safetyScore)
    }

    @Test
    fun `rejects rewrite when word count exceeds 20 percent above original`() {
        val original = "The rabbit ran."
        val rewrite = "The little rabbit ran very fast through the green forest and jumped over the fence."
        val ex = assertThrows(SafetyValidationException::class.java) {
            validator.validate(
                SafetyValidationRequest(
                    originalContent = original,
                    originalMoral = null,
                    formattedScript = rewrite,
                    age = 5,
                    language = "en"
                )
            )
        }
        assertTrue(ex.violations.any { it.contains("Word count") })
    }

    @Test
    fun `rejects rewrite when word count falls below 80 percent of original`() {
        val original = "The little rabbit ran through the forest and found a carrot."
        val rewrite = "Rabbit ran."
        val ex = assertThrows(SafetyValidationException::class.java) {
            validator.validate(
                SafetyValidationRequest(
                    originalContent = original,
                    originalMoral = null,
                    formattedScript = rewrite,
                    age = 5,
                    language = "en"
                )
            )
        }
        assertTrue(ex.violations.any { it.contains("Word count") })
    }

    @Test
    fun `rejects rewrite containing blocklist keyword`() {
        val original = "The rabbit was happy."
        val rewrite = "The rabbit was happy but then encountered a badword situation."
        val ex = assertThrows(SafetyValidationException::class.java) {
            validator.validate(
                SafetyValidationRequest(
                    originalContent = original,
                    originalMoral = null,
                    formattedScript = rewrite,
                    age = 5,
                    language = "en"
                )
            )
        }
        assertTrue(ex.violations.any { it.contains("Unsafe keyword") })
    }

    @Test
    fun `rejects rewrite when moral not preserved`() {
        val original = "The rabbit shared its carrot."
        val originalMoral = "Sharing with friends makes everyone happy."
        val rewrite = "The rabbit ate the carrot alone. The end."  // Moral words missing
        val ex = assertThrows(SafetyValidationException::class.java) {
            validator.validate(
                SafetyValidationRequest(
                    originalContent = original,
                    originalMoral = originalMoral,
                    formattedScript = rewrite,
                    age = 5,
                    language = "en"
                )
            )
        }
        assertTrue(ex.violations.any { it.contains("Moral") })
    }

    @Test
    fun `accepts rewrite when moral preserved`() {
        val original = "The rabbit shared its food with the mouse."
        val originalMoral = "Sharing"
        // Rewrite within 20% word count (7 words -> 6-8) and preserves moral (contains "Sharing")
        val rewrite = "The rabbit shared its food with the mouse. Sharing."
        val result = validator.validate(
            SafetyValidationRequest(
                originalContent = original,
                originalMoral = originalMoral,
                formattedScript = rewrite,
                age = 5,
                language = "en"
            )
        )
        assertTrue(result.valid)
    }
}
