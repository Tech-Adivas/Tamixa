package com.tamixa.application.translation

import com.tamixa.application.port.TranslatedContent
import com.tamixa.application.port.TranslationClientPort
import com.tamixa.infrastructure.config.AppProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Translated (or copied) titles end up in the target language, even when the model or the source gets it wrong. */
class TranslationServiceTitleLanguageTest {

    private val client: TranslationClientPort = mock()
    private val service = TranslationService(client, AppProperties(), null)

    @Test
    fun `same-language copy translates a title typed in another language`() {
        whenever(client.translate(eq("en"), eq("ta"), eq("The Enchantment of Friendship"), any())).thenReturn("நட்பின் மகிமை")

        val result = service.translateIfNeeded("ta", "ta", "The Enchantment of Friendship", "மாலை வானம்", null)

        assertThat(result.title).isEqualTo("நட்பின் மகிமை")
        assertThat(result.content).isEqualTo("மாலை வானம்")
    }

    @Test
    fun `title already in the language is kept without a model call`() {
        val result = service.translateIfNeeded("ta", "ta", "நட்பின் மகிமை", "மாலை வானம்", null)

        assertThat(result.title).isEqualTo("நட்பின் மகிமை")
        verify(client, never()).translate(any(), any(), any(), any())
    }

    @Test
    fun `model title in the wrong script is replaced by a translation of the original title`() {
        whenever(client.translateStructured(eq("en"), eq("ta"), anyOrNull(), any(), anyOrNull(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(TranslatedContent(title = "రక్షణ మరియు సేవింగ్", content = "பாதுகாப்பு கதை", moral = null))
        whenever(client.translate(eq("en"), eq("ta"), eq("Protection and Savings"), any())).thenReturn("பாதுகாப்பும் சேமிப்பும்")

        val result = service.translateIfNeeded("en", "ta", "Protection and Savings", "A story", null, bypassCache = true)

        assertThat(result.title).isEqualTo("பாதுகாப்பும் சேமிப்பும்")
    }

    @Test
    fun `test-translator tag is dropped before translating`() {
        // Story 72's English row: the test translator only prefixed the Tamil title.
        whenever(client.translate(eq("ta"), eq("en"), eq("நட்பின் மகிமை"), any())).thenReturn("The Glory of Friendship")

        assertThat(service.titleInLanguage("[en] நட்பின் மகிமை", "en")).isEqualTo("The Glory of Friendship")
    }

    @Test
    fun `title that cannot be fixed returns null`() {
        whenever(client.translate(any(), any(), any(), any())).thenReturn("still English")

        assertThat(service.titleInLanguage("Protection and Savings", "ta")).isNull()
    }
}
