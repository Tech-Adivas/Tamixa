package com.tamixa.infrastructure.logging

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PiiMaskTest {

    @Test
    fun maskEmailsInFreeText_replacesSubstrings() {
        val raw = "Contact parent@example.com or foo.bar+baz@domain.co.uk for help."
        val out = PiiMask.maskEmailsInFreeText(raw)
        assertThat(out).doesNotContain("parent@example.com")
        assertThat(out).doesNotContain("foo.bar+baz@domain.co.uk")
        assertThat(out).contains("@example.com")
        assertThat(out).contains("@domain.co.uk")
    }

    @Test
    fun maskEmailsInFreeText_emptyUnchanged() {
        assertThat(PiiMask.maskEmailsInFreeText("")).isEmpty()
    }
}
