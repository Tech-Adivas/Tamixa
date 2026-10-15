package com.tamixa.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** Titles must be written in their story's language; these are the cases found in the library. */
class ScriptLanguageTest {

    @Test
    fun `titles in the right language match`() {
        assertThat(ScriptLanguage.matches("நட்பின் மகிமை", "ta")).isTrue
        assertThat(ScriptLanguage.matches("The Enchantment of Friendship", "en")).isTrue
        assertThat(ScriptLanguage.matches("दोस्ती का जादू", "hi")).isTrue
        assertThat(ScriptLanguage.matches("UPI மோசடி", "ta")).isTrue // Latin brand words are fine
        assertThat(ScriptLanguage.matches(null, "ta")).isTrue
    }

    @Test
    fun `titles in another language are caught`() {
        assertThat(ScriptLanguage.matches("రక్షణ మరియు సేవింగ్", "ta")).isFalse // Telugu on a Tamil story (story 67)
        assertThat(ScriptLanguage.matches("The Enchantment of Friendship", "ta")).isFalse // English on Tamil (story 72)
        assertThat(ScriptLanguage.matches("[en] நட்பின் மகிமை", "en")).isFalse // Tamil on English (story 72)
        assertThat(ScriptLanguage.matches("[DSG] Ep01 — The Midnight Blackout (hi)", "hi")).isFalse
    }

    @Test
    fun `message names both languages`() {
        assertThat(ScriptLanguage.titleMismatchMessage("రక్షణ మరియు సేవింగ్", "ta"))
            .startsWith("Title is in Telugu but the story is Tamil")
        assertThat(ScriptLanguage.detect("The title")).isEqualTo("en")
    }
}
