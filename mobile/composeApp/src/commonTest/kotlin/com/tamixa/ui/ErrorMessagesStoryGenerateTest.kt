package com.tamixa.ui

import com.tamixa.network.StoryGenerateBadRequestException
import com.tamixa.ui.strings.Strings
import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorMessagesStoryGenerateTest {

    @Test
    fun unknownGenerationTopic_mapsToLocalizedMessage() {
        Strings.setLanguage("en")
        val msg = errorMessageForUser(
            StoryGenerateBadRequestException("UNKNOWN_GENERATION_TOPIC", "Unknown generationTopicId")
        )
        assertEquals(Strings.storyGenerateUnknownTopic(), msg)
    }

    @Test
    fun tamilOnly_mapsToLocalizedMessage() {
        Strings.setLanguage("ta")
        val msg = errorMessageForUser(
            StoryGenerateBadRequestException("GENERATION_LANGUAGE_NOT_SUPPORTED", "AI story generation is currently Tamil only")
        )
        assertEquals(Strings.storyGenerateTamilOnly(), msg)
    }

    @Test
    fun unknownCode_fallsBackToServerMessage() {
        Strings.setLanguage("en")
        val msg = errorMessageForUser(
            StoryGenerateBadRequestException("OTHER", "Custom server text")
        )
        assertEquals("Custom server text", msg)
    }
}
