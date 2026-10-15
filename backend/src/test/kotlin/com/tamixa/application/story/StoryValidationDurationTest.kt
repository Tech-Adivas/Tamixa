package com.tamixa.application.story

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

/** The model's estimated duration is metadata: a wrong guess is corrected, never a reason to reject the story. */
class StoryValidationDurationTest {

    private val validation = StoryValidation(
        maxWords = 900,
        safetyMiddleware = mock { on { isGeneratedContentChildSafe(any()) } doReturn true },
        safetyValidationRules = mock(),
        storyPromptBuilder = mock { on { maxWordsForAge(any()) } doReturn 900 }
    )

    private fun payload(words: Int, seconds: Int) = StructuredStoryPayload(
        title = "முயலின் நண்பன்",
        moral = "நட்பு முக்கியம்",
        storyText = List(words) { "சொல்" }.joinToString(" "),
        estimatedDurationSeconds = seconds
    )

    @Test
    fun `a far-off duration estimate is replaced by the word-count duration`() {
        // 256 words at 120 wpm = 128 s; the model said 550 s (seen with a real Tamil story).
        val result = validation.validate(payload(words = 256, seconds = 550), age = 5)

        assertThat(result.estimatedDurationSeconds).isEqualTo(128)
        assertThat(result.storyText).isEqualTo(payload(256, 550).storyText)
    }

    @Test
    fun `an estimate within tolerance is kept`() {
        val original = payload(words = 256, seconds = 140)

        assertThat(validation.validate(original, age = 5)).isEqualTo(original)
    }

    @Test
    fun `real problems still reject the story`() {
        assertThatThrownBy { validation.validate(payload(words = 5, seconds = 3), age = 5) }
            .isInstanceOf(InvalidStoryRequestException::class.java)
    }
}
