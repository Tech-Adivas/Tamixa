package com.tamixa.application.stream

import com.tamixa.infrastructure.config.AppProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

/** Signed links to cloned-voice audio: only the exact file, only until the link expires. */
class AudioLinkSignerTest {

    private val signer = AudioLinkSigner(AppProperties(audio = AppProperties.AudioProperties(linkSigningSecret = "test-secret", linkExpiryMinutes = 60)))
    private val key = "stories/71/ta/cloned_12.mp3"
    private val now = Instant.parse("2026-10-13T06:00:00Z")

    @Test
    fun `only cloned-voice files need a signature`() {
        assertThat(signer.requiresSignature(key)).isTrue
        assertThat(signer.requiresSignature("stories/71/ta/v1.mp3")).isFalse
        assertThat(signer.requiresSignature("stories/71/ta/calm.mp3")).isFalse
    }

    @Test
    fun `signed link is valid until it expires`() {
        val (exp, sig) = parse(signer.signatureQuery(key, now))

        assertThat(signer.isValid(key, exp, sig, now.plusSeconds(59 * 60))).isTrue
        assertThat(signer.isValid(key, exp, sig, now.plusSeconds(61 * 60))).isFalse
    }

    @Test
    fun `signature does not work for another file or a changed expiry`() {
        val (exp, sig) = parse(signer.signatureQuery(key, now))

        assertThat(signer.isValid("stories/71/ta/cloned_13.mp3", exp, sig, now)).isFalse
        assertThat(signer.isValid(key, (exp.toLong() + 3600).toString(), sig, now)).isFalse
        assertThat(signer.isValid(key, exp, null, now)).isFalse
        assertThat(signer.isValid(key, null, sig, now)).isFalse
    }

    @Test
    fun `links signed with another secret are refused`() {
        val other = AudioLinkSigner(AppProperties(audio = AppProperties.AudioProperties(linkSigningSecret = "other-secret")))
        val (exp, sig) = parse(other.signatureQuery(key, now))

        assertThat(signer.isValid(key, exp, sig, now)).isFalse
    }

    private fun parse(query: String): Pair<String, String> {
        val params = query.split('&').associate { it.substringBefore('=') to it.substringAfter('=') }
        return params.getValue("exp") to params.getValue("sig")
    }
}
